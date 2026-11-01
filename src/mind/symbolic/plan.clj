(ns mind.symbolic.plan
  "Planning as search through possible worlds.

  Newell & Simon's General Problem Solver (1959) introduced means–ends
  analysis: look at the difference between where you are and where you want
  to be, and choose an action that shrinks it. STRIPS (Fikes & Nilsson,
  1971), written for the robot Shakey, gave actions a simple form: the
  preconditions they need, the facts they add to the world, and the facts
  they delete from it.

  A world state here is just a set of triples. An action is a function from
  one such set to another.

  Search is exhaustive and reliable but blind, and the number of possible
  plans grows explosively. So this planner accepts an `intuition`: a function
  that says how promising a state *feels*. In mind.core that function is an
  LLM. The planner still checks that every step is legal. AlphaGo has the
  same structure: a learned hunch steering an exact search."
  (:require [clojure.set :as set]))

(defn applicable?
  [action state]
  (set/subset? (:pre action) state))

(defn perform
  "The world after an action: delete what it undoes, add what it does."
  [action state]
  (-> state
      (set/difference (:del action))
      (set/union (:add action))))

(defn difference
  "Means–ends analysis in a single line: what's still missing?"
  [goal state]
  (set/difference goal state))

(defn- cost
  "A* scoring: steps taken so far, plus the remaining difference, minus
  however hopeful intuition feels."
  [{:keys [state steps]} goal intuition]
  (- (+ (count steps)
        (count (difference goal state)))
     (intuition state goal)))

(defn plan
  "Best-first search from start to a state where goal holds. Returns the
  list of action names, or nil if no plan exists."
  [start goal actions & {:keys [intuition] :or {intuition (constantly 0)}}]
  (loop [frontier [{:state start :steps []}]
         visited  #{}]
    (when-let [[{:keys [state steps]} & others] (seq frontier)]
      (cond
        (empty? (difference goal state)) steps
        (visited state)                  (recur others visited)
        :else
        (let [successors (for [a actions :when (applicable? a state)]
                           {:state (perform a state)
                            :steps (conj steps (:name a))})]
          (recur (sort-by #(cost % goal intuition) (concat others successors))
                 (conj visited state)))))))
