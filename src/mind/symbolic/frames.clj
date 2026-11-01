(ns mind.symbolic.frames
  "Frames: knowledge as stereotypes with defaults.

  Marvin Minsky (1974) argued that we don't reason from bare facts. We
  reason from remembered stereotypes: a 'bird' frame with slots
  (covering: feathers, flies: yes) that we fill in and override as we learn
  more. Tweety is a bird, so Tweety flies. Unless Tweety is a penguin, in
  which case the more specific frame wins.

  This is non-monotonic reasoning: learning more can make you believe
  *less*. Classical logic can't do this, and people do it constantly. (Try
  it with the rules engine: a rule saying 'members of a kind share its
  traits' will cheerfully conclude that Tweety both flies and doesn't.)

  The frames live in memory as ordinary triples:

      [:penguin :ako :bird]      ; a kind of
      [:penguin :flies :`no]
      [:tweety :is-a :penguin]"
  (:require [mind.memory :as mem]))

(def ^:private structural
  "Slots that link frames together, as opposed to describing them."
  [:is-a :ako])

(defn- parent
  "We climb only links that were *stated*. Reflection may have added the
  shortcut [:tweety :is-a :bird], and taking it would jump straight past
  the penguin. Choosing the most specsific route is the 'specificity'
  problem of inheritance networks."
  [db frame]
  (or (mem/stated db frame :ako)
      (mem/stated db frame :is-a)))

(defn lineage
  "A frame, then its parent, then its grandparent: most specific first."
  [db frame]
  (take-while some? (iterate #(parent db %) frame)))

(defn slot-value
  "Ask a frame for a slot. If it doesn't say, ask its parent, and so on up.
  The first answer found, which is the most specific, wins."
  [db frame slot]
  (some #(mem/value db % slot) (lineage db frame)))

(defn expectations
  "Everything we'd assume about a thing knowing only what kind of thing it
  is. General defaults go in first; specific ones overwrite them."
  [db frame]
  (as-> (lineage db frame) $
    (reverse $)
    (map #(mem/slots db %) $)
    (apply merge $)
    (apply dissoc $ structural)))

;; Not handled: multiple inheritance. Nixon was a Quaker (pacifist by default)
;; and a Republican (not a pacifist by default). Which frame wins? There is no
;; agreed answer. The 'Nixon diamond' is still a standard puzzle in
;; default reasoning.
