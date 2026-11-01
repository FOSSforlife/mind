(ns mind.memory
  "Long-term memory as a web of triples.

  Every belief is a tiny sentence: subject, predicate, object.

      [:socrates :is-a :man]

  Semantic networks (Quillian, 1968) have this shape, and so do the Semantic
  Web's RDF and DataScript, whose 'datoms' are triples plus a little
  bookkeeping.

  Each triple is stored as an entity of its own, so we can say things
  *about* a belief: where it came from, how sure we are of it, and what it
  was inferred from. A mind that can't distinguish what it was told from what
  it worked out, or from what it only guessed, has no epistemology.

  Sources of belief:
    :background   what the mind started with
    :testimony    what it was told (and understood via the LLM)
    :inference    what it worked out for itself
    :conjecture   what it guessed, tentatively"
  (:require [datascript.core :as d]))

(def schema
  {:belief/triple {:db/unique :db.unique/identity}})

(defn create
  []
  (d/create-conn schema))

(defn believe!
  [conn [s p o :as triple] {:keys [source confidence because]
                            :or   {confidence 1.0}}]
  (d/transact! conn
               [(cond-> {:belief/triple     triple
                         :belief/subject    s
                         :belief/predicate  p
                         :belief/object     o
                         :belief/source     source
                         :belief/confidence confidence}
                  because (assoc :belief/because because))]))

(defn retract!
  "Stop believing something.

  A full truth maintenance system (Doyle, 1979) would also withdraw
  everything that was believed *only* because of it, following the
  :belief/because links downward. That is left as a stub."
  [conn triple]
  (d/transact! conn [[:db/retractEntity [:belief/triple triple]]]))

;; ── Asking memory questions ──────────────────────────────────────────────

(defn facts
  "Every believed triple, as a set."
  [db]
  (set (d/q '[:find ?s ?p ?o
              :where [?b :belief/subject ?s]
                     [?b :belief/predicate ?p]
                     [?b :belief/object ?o]]
            db)))

(defn value
  "What does memory say s's p is?"
  [db s p]
  (d/q '[:find ?o .
         :in $ ?s ?p
         :where [?b :belief/subject ?s]
                [?b :belief/predicate ?p]
                [?b :belief/object ?o]]
       db s p))

(defn stated
  "Like value, but only counting what was stated, not inferred."
  [db s p]
  (d/q '[:find ?o .
         :in $ ?s ?p
         :where [?b :belief/subject ?s]
                [?b :belief/predicate ?p]
                [?b :belief/object ?o]
                [?b :belief/source ?src]
                [(not= ?src :inference)]]
       db s p))

(defn slots
  "Everything memory says directly about s, as a map."
  [db s]
  (into {} (d/q '[:find ?p ?o
                  :in $ ?s
                  :where [?b :belief/subject ?s]
                         [?b :belief/predicate ?p]
                         [?b :belief/object ?o]]
                db s)))

(defn provenance
  "Where did this belief come from, and how much is it trusted?"
  [db triple]
  (d/pull db [:belief/source :belief/confidence :belief/because]
          [:belief/triple triple]))

(defn confidence
  [db triple]
  (:belief/confidence (provenance db triple) 1.0))

(defn vocabulary
  "The concepts this mind has words for. The LLM is told to use these, so
  that what it hears connects to what the mind already knows. Getting a
  model to stick to the ontology is a large part of the real engineering."
  [db]
  (set (d/q '[:find [?term ...]
              :where (or [_ :belief/predicate ?term]
                         [_ :belief/object ?term])]
            db)))
