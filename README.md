# mind

A small, readable model of a hybrid AI: an **LLM** for language and intuition,
a **triple store** for memory, and the **symbolic algorithms** of classical AI
for reasoning.

```
words ──LLM──▶ triples ──▶ memory ◀──▶ rules · proofs · frames · plans
                                                    │
words ◀──LLM── explanation ◀────────────────────────┘
```

> The LLM proposes; the symbols decide.

This is meant to be read, not deployed. Every LLM call is stubbed with a
canned reply so that the structure stays visible. The symbolic parts are
real, though: they are short, and their shortness is part of the point.

```sh
clojure -M:run
```

## The idea

Kahneman described two modes of thought. **System 1** is fast, associative and
fluent, and can't explain itself. **System 2** is slow and deliberate, and it
can show its work. An LLM resembles System 1. Lisp-era AI built System 2 and
ran into trouble with what System 1 does easily: understanding language,
coming up with hypotheses, telling which options look promising.

So this mind gives each part the job it suits:

| The LLM does…                       | The symbols do…                                   |
| ----------------------------------- | ------------------------------------------------- |
| read language into triples          | store them, along with where each one came from   |
| guess hypotheses (abduction)        | check the guesses for consistency, keep or reject |
| sense which plans look promising    | search exhaustively, and only take legal steps    |
| turn proofs into prose              | produce the proofs                                |

## Reading order

| File | Idea | Lineage |
| --- | --- | --- |
| `symbolic/unify.clj`  | Pattern matching with variables, the shared engine | Robinson 1965 |
| `symbolic/rules.clj`  | IF–THEN rules, forward chaining to a fixpoint | OPS5, MYCIN, SOAR, ACT-R |
| `symbolic/prove.clj`  | Backward chaining; proofs as explanations | Prolog 1972 |
| `symbolic/frames.clj` | Stereotypes with defaults; non-monotonic reasoning | Minsky 1974 |
| `symbolic/plan.clj`   | Means–ends analysis, STRIPS actions, A* | GPS 1959, Shakey 1971 |
| `memory.clj`          | Beliefs as triples, with provenance and confidence | Quillian 1968, RDF, DataScript |
| `llm.clj`             | The stubbed model: parse, guess, intuit, speak | — |
| `knowledge.clj`       | Ontology, rules, and actions: what the mind starts out knowing | McCarthy 1963 |
| `core.clj`            | The cognitive cycle that connects everything | — |

Start with `core.clj` if you want the overall story first. Start with
`unify.clj` if you want to build it up from the bottom.

## What the demo shows

1. **Perceiving.** "Socrates was a man of Athens" becomes `[:socrates :is-a :man]`,
   stored as *testimony* with confidence 0.97, not as plain fact.
2. **Deliberating.** "Is Socrates mortal?" is answered by a proof tree, and every
   leaf is traced to its source.
3. **Reflecting.** Forward chaining works out everything that follows.
4. **Expecting.** Tweety is a bird, and birds fly, but Tweety is a penguin, so
   Tweety doesn't. More specific knowledge overrides defaults.
5. **Guessing.** "Is Socrates wise?" can't be proven. The LLM offers two
   hypotheses: *philosopher* and *god*. Logic rejects *god* because Socrates is
   already known to be mortal. The answer goes through, flagged as resting on
   a conjecture.
6. **Acting.** The monkey gets the bananas.

## Questions worth asking of the code

- **The symbol grounding problem** (Harnad, 1990). `:socrates` means something
  only because of its links to other symbols. Is the LLM's stock of text
  enough to ground them? Is anything?
- **The Chinese Room** (Searle, 1980). `llm/complete` returns canned answers
  and understands nothing, yet the system around it works. Where in this
  program, if anywhere, does understanding happen?
- **The frame problem** (McCarthy & Hayes, 1969). STRIPS actions state what
  they change and assume everything else stays the same. How does a mind
  know what *doesn't* change?
- **Epistemology.** `memory.clj` separates background, testimony, inference
  and conjecture. Is that enough? What would a belief about one's own beliefs
  look like?
- **Defaults and the Nixon diamond.** See the note at the end of `frames.clj`.

## Stubs and possible extensions

- `llm/complete`: replace it with a real API call, and parse EDN out of the
  replies.
- `memory/retract!`: make it a real truth maintenance system that also withdraws
  every belief that depended on the retracted one.
- `llm/hunch`: always returns 0, so planning falls back on means–ends analysis
  alone.
- Not included: a blackboard architecture (Hearsay-II), where many
  specialists, the LLM among them, post partial results to a shared memory.
  It would fit naturally on top of `memory.clj`.
