(ns mind.core
  "A small model of a mind: an LLM for intuition and language, a triple
  store for memory, and the classical algorithms of symbolic AI for
  reasoning.

  The core idea is how the work is divided:

      words ──LLM──▶ triples ──▶ memory ◀──▶ rules · proofs · frames · plans
                                                          │
      words ◀──LLM── explanation ◀────────────────────────┘

  The LLM proposes; the symbols decide."
  (:require [clojure.pprint :refer [pprint]]
            [mind.knowledge :as knowledge]
            [mind.llm :as llm]
            [mind.memory :as mem]
            [mind.symbolic.frames :as frames]
            [mind.symbolic.plan :as plan]
            [mind.symbolic.prove :as prove]
            [mind.symbolic.rules :as rules]))

(defn new-mind
  []
  (let [memory (mem/create)]
    (doseq [triple knowledge/background]
      (mem/believe! memory triple {:source :background}))
    {:memory memory
     :rules  knowledge/rules}))

;; ── Perceiving ───────────────────────────────────────────────────────────

(defn perceive
  "Hear something, understand it, and remember it, filed as testimony
  rather than as truth."
  [{:keys [memory]} text]
  (doseq [{:keys [triple confidence]} (llm/read-triples text (mem/vocabulary @memory))]
    (mem/believe! memory triple {:source :testimony :confidence confidence})))

;; ── Reflecting ───────────────────────────────────────────────────────────

(defn reflect
  "Work out every consequence of what is believed. A conclusion is only as
  strong as its weakest premise (a rough version of MYCIN's certainty
  factors)."
  [{:keys [memory rules]}]
  (let [derived (rules/forward-chain rules (mem/facts @memory))]
    (doseq [{:keys [fact because]} derived]
      (mem/believe! memory fact
                    {:source     :inference
                     :because    because
                     :confidence (apply min (map #(mem/confidence @memory %)
                                                 (:premises because)))}))
    (map :fact derived)))

;; ── Answering ────────────────────────────────────────────────────────────

(defn- attempt-proof
  [{:keys [memory rules]} goal]
  (when-let [{:keys [proof]} (first (prove/prove goal (mem/facts @memory) rules))]
    {:goal goal :answer :yes :proof proof}))

(defn- hypothesize!
  "When nothing explains the goal, ask the LLM for guesses and keep only
  those consistent with everything already believed. The ones that survive
  are entertained, not believed outright."
  [{:keys [memory rules]} goal]
  (let [facts       (mem/facts @memory)
        consistent? #(rules/consistent? rules (conj facts (:triple %)))
        {kept true rejected false} (group-by consistent? (llm/conjecture goal facts))]
    (doseq [{:keys [triple confidence]} kept]
      (mem/believe! memory triple {:source :conjecture :confidence confidence}))
    {:entertained kept :rejected rejected}))

(defn ask
  "Answer by proof if possible. If not, guess, check the guess, and try
  the proof again."
  [mind question]
  (let [goal (llm/understand-question question)]
    (or (attempt-proof mind goal)
        (let [guesses (hypothesize! mind goal)]
          (-> (or (attempt-proof mind goal) {:goal goal :answer :unknown})
              (assoc :guesses guesses))))))

(defn explain
  "Put the reasoning into words, along with the source of every premise."
  [{:keys [memory]} question {:keys [proof]}]
  (let [grounds (into {} (for [leaf (prove/leaves proof)]
                           [leaf (mem/provenance @memory leaf)]))]
    (llm/articulate question proof grounds)))

;; ── Expecting and acting ─────────────────────────────────────────────────

(defn expect
  "What should I assume about this thing until I learn otherwise?"
  [{:keys [memory]} thing]
  (frames/expectations @memory thing))

(defn achieve
  "Find a sequence of actions that reaches the goal. Exact search does the
  work, and LLM intuition decides where to look first."
  [start goal]
  (plan/plan start goal knowledge/actions :intuition llm/hunch))

;; ── A day in the life ────────────────────────────────────────────────────

(defn- section [title]
  (println (str "\n━━ " title " " (apply str (repeat (- 66 (count title)) "━")) "\n")))

(defn -main [& _]
  (let [mind (new-mind)]

    (section "1 · Perceiving: words become beliefs")
    (perceive mind "Socrates was a man of Athens. Tweety, by the way, is a penguin.")
    (println "Told: \"Socrates was a man of Athens. Tweety, by the way, is a penguin.\"")
    (println "Now believes, on testimony:")
    (doseq [t [[:socrates :is-a :man] [:socrates :born-in :athens] [:tweety :is-a :penguin]]]
      (println "   " t (mem/provenance @(:memory mind) t)))

    (section "2 · Deliberating: is Socrates mortal?")
    (let [q "Is Socrates mortal?"
          answer (ask mind q)]
      (print (prove/render (:proof answer)))
      (println)
      (println (explain mind q answer)))

    (section "3 · Reflecting: what follows from all this?")
    (doseq [fact (reflect mind)]
      (println "   " fact))

    (section "4 · Expecting: what is Tweety like?")
    (println "Birds fly, but penguins don't, and the more specific frame wins:")
    (pprint (expect mind :tweety))

    (section "5 · Guessing: is Socrates wise?")
    (let [q "Is Socrates wise?"
          {:keys [guesses] :as answer} (ask mind q)]
      (println "Nothing I know proves it, so I asked for hypotheses:")
      (doseq [{:keys [triple rationale]} (:entertained guesses)]
        (println "   entertained" triple "—" rationale))
      (doseq [{:keys [triple rationale]} (:rejected guesses)]
        (println "   rejected   " triple "—" rationale "(contradicts what I know)"))
      (println)
      (print (prove/render (:proof answer)))
      (println)
      (println (explain mind q answer)))

    (section "6 · Acting: the monkey and the bananas")
    (println "Plan:" (achieve knowledge/monkey-start knowledge/monkey-goal))))

(comment
  ;; A REPL tour. Evaluate one form at a time.
  (def m (new-mind))
  (perceive m "Socrates was a man of Athens.")
  (mem/facts @(:memory m))
  (-> (ask m "Is Socrates mortal?") :proof prove/render println)
  (reflect m)
  (expect m :tweety)
  (frames/slot-value @(:memory m) :tweety :flies)
  (achieve knowledge/monkey-start knowledge/monkey-goal))
