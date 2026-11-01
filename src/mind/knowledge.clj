(ns mind.knowledge
  "What the mind knows before it hears anything: a small ontology, a few
  rules of inference, and a repertoire of actions.

  Classical AI hand-wrote all of this, and the 'knowledge acquisition
  bottleneck' was the main reason it failed to scale. Today an LLM could
  draft much of it, which mind.core would then check before believing."
  (:require [mind.symbolic.rules :refer [defrule]]))

;; ── Background beliefs: an ontology, also used as frames ─────────────────

(def background
  [[:man         :ako :human]
   [:philosopher :ako :human]
   [:human       :ako :animal]
   [:penguin     :ako :bird]
   [:bird        :ako :animal]
   [:god         :ako :immortal]

   [:animal  :alive    :yes]
   [:bird    :covering :feathers]
   [:bird    :flies    :yes]
   [:penguin :flies    :no]
   [:penguin :habitat  :antarctica]])

;; ── Rules of inference ───────────────────────────────────────────────────

(defrule kinds-inherit
  "Whatever is a kind of thing is also a member of the larger kind."
  [?x :is-a ?kind]
  [?kind :ako ?larger]
  =>
  [?x :is-a ?larger])

(defrule mortality
  "Every animal is mortal."
  [?x :is-a :animal]
  =>
  [?x :is-a :mortal])

(defrule love-of-wisdom
  "Philosophers are, at least by profession, wise."
  [?x :is-a :philosopher]
  =>
  [?x :has-trait :wisdom])

(defrule nothing-is-both
  "An integrity constraint: nothing is both mortal and immortal."
  [?x :is-a :mortal]
  [?x :is-a :immortal]
  =>
  [:contradiction :concerning ?x])

(def rules
  [kinds-inherit mortality love-of-wisdom nothing-is-both])

;; ── Actions: the monkey and the bananas (McCarthy, 1963) ─────────────────
;;
;; Bananas hang from the ceiling at the centre of the room, out of reach.
;; A box stands by the window. The monkey is at the door.

(def monkey-start
  #{[:monkey  :at :door]
    [:monkey  :on :floor]
    [:box     :at :window]
    [:bananas :at :center]})

(def monkey-goal
  #{[:monkey :has :bananas]})

(def actions
  [{:name :walk-to-window
    :pre  #{[:monkey :at :door]}
    :add  #{[:monkey :at :window]}
    :del  #{[:monkey :at :door]}}

   {:name :walk-to-center
    :pre  #{[:monkey :at :door]}
    :add  #{[:monkey :at :center]}
    :del  #{[:monkey :at :door]}}

   {:name :push-box-to-center
    :pre  #{[:monkey :at :window] [:box :at :window] [:monkey :on :floor]}
    :add  #{[:monkey :at :center] [:box :at :center]}
    :del  #{[:monkey :at :window] [:box :at :window]}}

   {:name :climb-box
    :pre  #{[:monkey :at :center] [:box :at :center] [:monkey :on :floor]}
    :add  #{[:monkey :on :box]}
    :del  #{[:monkey :on :floor]}}

   {:name :grasp-bananas
    :pre  #{[:monkey :on :box] [:box :at :center] [:bananas :at :center]}
    :add  #{[:monkey :has :bananas]}
    :del  #{}}])
