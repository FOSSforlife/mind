(ns mind.symbolic.rules
  "Production rules and forward chaining.

  IF these patterns hold, THEN these conclusions follow. This was the design
  of the expert systems of the 1970s and 80s (MYCIN, OPS5, XCON). It was also
  the basis of cognitive architectures like Newell's SOAR and Anderson's
  ACT-R, which proposed that human skill is itself a large set of rules like
  these.

  Forward chaining works from the data: start with what you know, fire every
  rule that matches, add what the rules conclude, and repeat until nothing
  new appears. One belief leads to the next, and so on."
  (:require [mind.symbolic.unify :as u]))

(defmacro defrule
  "Write a rule the way it would read on a whiteboard:

      (defrule mortality
        \"Every animal is mortal.\"
        [?x :is-a :animal]
        =>
        [?x :is-a :mortal])"
  [rule-name doc & body]
  (let [[premises [_arrow & conclusions]] (split-with #(not= '=> %) body)]
    `(def ~rule-name
       {:name ~(keyword rule-name)
        :doc  ~doc
        :if   '~(vec premises)
        :then '~(vec conclusions)})))

(defn- match-all
  "Every set of bindings under which all the premises are known facts."
  [premises facts]
  (reduce (fn [binding-sets premise]
            (for [bs   binding-sets
                  fact facts
                  :let [bs' (u/unify premise fact bs)]
                  :when bs']
              bs'))
          [{}]
          premises))

(defn fire
  "Every new conclusion one rule can draw from the facts. Each conclusion
  records why it is believed, so the reasoning can be explained later."
  [rule facts]
  (for [bs         (match-all (:if rule) facts)
        conclusion (:then rule)
        :let [fact (u/substitute conclusion bs)]
        :when (not (contains? facts fact))]
    {:fact    fact
     :because {:rule     (:name rule)
               :premises (mapv #(u/substitute % bs) (:if rule))}}))

(defn forward-chain
  "The recognize–act cycle: fire every rule, add the conclusions, and repeat
  until nothing new appears. Returns each newly derived fact with its
  justification, in the order it was derived."
  [rules facts]
  (loop [facts (set facts), derived []]
    (let [new (->> rules
                   (mapcat #(fire % facts))
                   (group-by :fact)
                   vals
                   (map first))]
      (if (empty? new)
        derived
        (recur (into facts (map :fact new))
               (into derived new))))))

(defn contradictions
  "Some rules are integrity constraints: they conclude [:contradiction ...]
  when beliefs clash. Run everything forward and see if any of them fire."
  [rules facts]
  (->> (forward-chain rules facts)
       (map :fact)
       (filter #(= :contradiction (first %)))))

(defn consistent?
  [rules facts]
  (empty? (contradictions rules facts)))
