(ns mind.symbolic.prove
  "Backward chaining: reasoning from a question back to whatever would
  answer it.

  Forward chaining asks 'what follows from what I know?' Backward chaining
  asks 'what would have to be true for this to be true?', then asks the same
  of each sub-question until every branch ends in a known fact. This is the
  strategy of Prolog (Colmerauer & Kowalski, 1972) and of MYCIN.

  The search leaves a trail behind it: a proof tree. So the system can say
  not only *that* something is true but *why*. An LLM working alone can only
  imitate that."
  (:require [clojure.string :as str]
            [mind.symbolic.unify :as u]))

(declare prove-all)

(defn prove
  "Lazily produce every way of establishing goal, each as
  {:bindings … :proof …}. The search has a depth limit, because some
  questions lead backwards forever."
  ([goal facts rules] (prove goal facts rules {} 6))
  ([goal facts rules bindings depth]
   (when (pos? depth)
     (concat
       ;; 1. Is it something I already know?
       (for [fact facts
             :let [bs (u/unify goal fact bindings)]
             :when bs]
         {:bindings bs
          :proof    {:goal (u/substitute goal bs)}})

       ;; 2. Is there a rule whose conclusion would give it to me?
       ;;    Then try to prove that rule's premises instead.
       (for [rule       (map u/rename rules)
             conclusion (:then rule)
             :let [bs (u/unify goal conclusion bindings)]
             :when bs
             sub (prove-all (:if rule) facts rules bs (dec depth))]
         {:bindings (:bindings sub)
          :proof    {:goal (u/substitute goal (:bindings sub))
                     :rule (:name rule)
                     :from (:proofs sub)}})))))

(defn prove-all
  "Prove several goals together, passing bindings from each to the next."
  [goals facts rules bindings depth]
  (if (empty? goals)
    [{:bindings bindings :proofs []}]
    (for [head (prove (first goals) facts rules bindings depth)
          tail (prove-all (rest goals) facts rules (:bindings head) depth)]
      {:bindings (:bindings tail)
       :proofs   (cons (:proof head) (:proofs tail))})))

(defn leaves
  "The facts a proof finally rests on: its premises, its ground."
  [proof]
  (if (:rule proof)
    (mapcat leaves (:from proof))
    [(:goal proof)]))

(defn render
  "Draw a proof as an indented tree, conclusion first."
  ([proof] (render proof 0))
  ([{:keys [goal rule from]} depth]
   (str (str/join (repeat depth "    "))
        (pr-str goal)
        (if rule (str "   ⟸ " (name rule)) "   ✓")
        "\n"
        (str/join (map #(render % (inc depth)) from)))))
