(ns mind.symbolic.unify
  "Unification: the small engine underneath symbolic AI.

  Two patterns unify if their blanks (variables) can be filled in so that
  they come out identical. J. A. Robinson described it in 1965 as the core
  of his resolution theorem prover. Prolog is built on it, and the pattern
  matcher in every production system is a simpler version of it.

  A variable is any symbol that starts with ?

      (unify '[?x :is-a :human] [:socrates :is-a :human])
      ;=> {?x :socrates}

  A set of bindings is just a map from variables to what they stand for.
  Failure is nil.

  (A complete unifier also does an 'occurs check', which refuses to bind
  ?x to something that contains ?x. Our terms are flat triples, so we
  leave it out.)"
  (:require [clojure.walk :as walk]))

(defn variable?
  [x]
  (and (symbol? x) (= \? (first (name x)))))

(defn resolve-term
  "Follow a chain of bindings (?x → ?y → :socrates) to the end."
  [term bindings]
  (if (and (variable? term) (contains? bindings term))
    (recur (get bindings term) bindings)
    term))

(defn unify
  "Find bindings that make a and b the same, extending any bindings
  already made. Returns nil if there are none."
  ([a b] (unify a b {}))
  ([a b bindings]
   (when bindings
     (let [a (resolve-term a bindings)
           b (resolve-term b bindings)]
       (cond
         (= a b)       bindings
         (variable? a) (assoc bindings a b)
         (variable? b) (assoc bindings b a)

         (and (sequential? a) (sequential? b) (= (count a) (count b)))
         (reduce (fn [bs [x y]] (unify x y bs)) bindings (map vector a b))

         :else nil)))))

(defn substitute
  "Fill in a pattern's blanks using the bindings."
  [pattern bindings]
  (walk/postwalk #(if (variable? %) (resolve-term % bindings) %) pattern))

(defn rename
  "Give a rule fresh variables each time it is used, so the ?x in one use
  can't collide with the ?x in another. Every Prolog does this quietly."
  [form]
  (let [fresh (memoize (fn [v] (gensym (str (name v) "_"))))]
    (walk/postwalk #(if (variable? %) (fresh %) %) form)))
