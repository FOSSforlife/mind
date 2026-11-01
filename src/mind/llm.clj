(ns mind.llm
  "The language model: fluent, associative, fast, and unable to show its work.

  In dual-process terms (Kahneman's System 1 and System 2), the LLM plays
  System 1. It produces plausible output instantly, drawing on a huge
  compressed memory of human text, but it can't say how it got there.

  So we use it only for what it does well:
    reading     turning messy language into clean triples   (parsing)
    guessing    proposing hypotheses when logic runs out     (abduction)
    intuiting   rating which path *feels* promising          (heuristics)
    speaking    turning a proof back into prose              (generation)
  and never for what it does badly: deciding what is true.

  Everything in this file is stubbed. `complete` is where a real API call
  would go. Each function shows the prompt it would send and the kind of
  answer it would expect back."
  (:require [clojure.string :as str]))

(defn- complete
  "Send a prompt to a model and get its reply. This stand-in ignores the
  prompt and returns the reply we pretend the model gave.

  That makes it a literal Chinese Room (Searle, 1980): the right symbols
  come out and nothing inside understands them. Whether the real model
  differs in kind or only in degree is the open question."
  [_prompt & {:keys [pretend]}]
  pretend)

;; ── Reading ──────────────────────────────────────────────────────────────

(defn read-triples
  "Parsing. Language is ambiguous and endlessly varied, and triples are
  neither. Turning one into the other was the long-standing goal of
  symbolic NLP. SHRDLU (Winograd, 1970) managed it inside a toy world of
  blocks. An LLM manages it reasonably well for most text."
  [text vocabulary]
  (complete
    (str "Translate the text into [subject predicate object] triples.\n"
         "Prefer these terms: " (pr-str vocabulary) "\n"
         "Give each triple a confidence between 0 and 1.\n\n"
         "Text: " text)
    :pretend [{:triple [:socrates :is-a :man]       :confidence 0.97}
              {:triple [:socrates :born-in :athens] :confidence 0.90}
              {:triple [:tweety :is-a :penguin]     :confidence 0.95}]))

(def ^:private phrasebook
  "Our stand-in model knows exactly two questions."
  {"Is Socrates mortal?" '[:socrates :is-a :mortal]
   "Is Socrates wise?"   '[:socrates :has-trait :wisdom]})

(defn understand-question
  "Turn a question into a pattern that logic can try to prove."
  [question]
  (complete
    (str "Rewrite this question as a single [subject predicate object] "
         "pattern. Use ?variables for anything unknown.\n\n"
         "Question: " question)
    :pretend (phrasebook question)))

;; ── Guessing ─────────────────────────────────────────────────────────────

(defn conjecture
  "Abduction (C. S. Peirce): when you can't prove something, guess what
  would make it true. Logic alone has no source of guesses. An LLM has read
  everything and has plenty, some good and some not. Logic then decides
  which ones to keep."
  [goal known-facts]
  (complete
    (str "I believe these facts:\n" (pr-str known-facts) "\n\n"
         "I cannot prove " (pr-str goal) ". Suggest facts that, if true, "
         "would make it provable. Give a confidence for each.")
    :pretend [{:triple    [:socrates :is-a :philosopher]
               :confidence 0.85
               :rationale "He is the most famous philosopher of Athens."}
              {:triple    [:socrates :is-a :god]
               :confidence 0.05
               :rationale "He spoke of a divine voice that guided him."}]))

;; ── Intuiting ────────────────────────────────────────────────────────────

(defn hunch
  "How promising does this situation feel? This is a learned heuristic,
  the part of expertise that's hard to put into words. The stub has no
  hunches, so it returns 0 and the planner falls back on means–ends
  analysis alone."
  [state goal]
  (complete
    (str "On a scale from 0 to 1, how close does this situation look to "
         "achieving the goal?\n\nSituation: " (pr-str state)
         "\nGoal: " (pr-str goal))
    :pretend 0))

;; ── Speaking ─────────────────────────────────────────────────────────────

(defn- like-an-expert-system
  "The reply we pretend to get back, written in the stiff style of a 1970s
  expert system, roughly how MYCIN explained itself. A real model would say
  the same thing in natural prose."
  [{:keys [goal rule]} grounds]
  (str "CONCLUSION: " (pr-str goal) " (by " (name rule) ")\n"
       (str/join "\n"
                 (for [[fact {:belief/keys [source confidence]}] grounds]
                   (format "  RESTS ON: %-32s [%s, %.2f]"
                           (pr-str fact) (name source) (double confidence))))
       (when (some #(= :conjecture (:belief/source (val %))) grounds)
         "\n  CAUTION: this depends on a conjecture, not on knowledge.")))

(defn articulate
  "Generation: turn a proof and its grounds into an honest answer. The
  model only rephrases the reasoning; it doesn't add any. That constraint is
  what keeps the explanation faithful to how the answer was actually
  reached."
  [question proof grounds]
  (complete
    (str "Answer the question in plain, warm English, using only this proof. "
         "Be candid about any step that rests on a guess.\n\n"
         "Question: " question "\n"
         "Proof: "    (pr-str proof) "\n"
         "Grounds: "  (pr-str grounds))
    :pretend (like-an-expert-system proof grounds)))
