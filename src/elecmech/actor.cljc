(ns elecmech.actor
  "ElecMechActor — the ISCO-08 7412 Electrical Mechanics and Fitters
  service scheduling/logistics coordination actor as a
  `langgraph.graph/state-graph` (ADR-2607121000 / CLAUDE.md Actors
  section). One graph run = one service-coordination operation request
  (intake -> advise -> govern -> decide -> commit/hold, with a
  human-approval interrupt for escalated proposals). No infinite
  internal loop; checkpointed per superstep so an interrupted run can
  resume after human sign-off. Modeled on cloud-itonami-isco-7127's
  hvacmech.actor (closest domain shape — same physical-safety-domain,
  certification-gated technician-safety pattern — itself modeled on
  cloud-itonami-isco-7111's housebuilder.actor,
  cloud-itonami-isco-3313's accountingsupport.actor and
  cloud-itonami-isco-9311's mininglabor.actor for the
  physical-safety-domain shape).

  ```text
  :intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                             +-> :request-approval   (:escalate? true, interrupt-before)
                                             +-> :hold               (:hard? true)
  ```

  The unconditional invariant: the Electrical Mechanic Advisor can
  never directly commit an operating record the ElecMechGovernor
  refuses, and can never make this actor finalize an
  electrical-repair-execution decision (e.g. proceeding with a
  specific electrical repair), authorize a lockout/tagout clearance,
  or override an electrical safety officer's judgment — every
  commit-record! call is gated behind `:decide`, and that decision
  class is a permanent hard block regardless of disposition path.
  This actor coordinates SERVICE SCHEDULING/LOGISTICS ONLY — it never
  performs electrical-repair work or authorizes electrical-repair
  execution itself."
  (:require [langgraph.graph :as g]
            [langgraph.checkpoint :as cp]
            [elecmech.advisor :as advisor]
            [elecmech.governor :as governor]
            [elecmech.store :as store]))

(defn build-graph
  "Build a compiled ElecMechActor graph. `store` implements
  `elecmech.store/Store`. `advisor` implements
  `elecmech.advisor/Advisor` (defaults to `mock-advisor`).
  `checkpointer` defaults to an in-memory one."
  [{:keys [store advisor checkpointer]
    :or {advisor (advisor/mock-advisor)
         checkpointer (cp/mem-checkpointer)}}]
  (-> (g/state-graph
       {:channels
        {:request     {:default nil}
         :context     {:default nil}
         :proposal    {:default nil}
         :verdict     {:default nil}
         :disposition {:default nil}
         :record      {:default nil}
         :audit       {:reducer into :default []}}})
      (g/add-node :intake (fn [s] s))
      (g/add-node :advise
                   (fn [{:keys [request]}]
                     (let [p (advisor/-advise advisor store request)]
                       {:proposal p
                        :audit [{:node :advise :request request :proposal p}]})))
      (g/add-node :govern
                   (fn [{:keys [request context proposal]}]
                     (let [v (governor/check request context proposal store)]
                       {:verdict v
                        :audit [{:node :govern :verdict v}]})))
      (g/add-node :decide
                   (fn [{:keys [verdict]}]
                     {:disposition (cond
                                     (:hard? verdict) :hold
                                     (:escalate? verdict) :request-approval
                                     :else :commit)}))
      (g/add-node :request-approval (fn [s] s))
      (g/add-node :commit
                   (fn [{:keys [request proposal]}]
                     (let [record {:technician-id (:technician-id request)
                                    :op (:op proposal)
                                    :service-account-id (:service-account-id proposal)
                                    :payload proposal}]
                       (store/commit-record! store record)
                       (store/append-ledger! store {:disposition :commit :record record})
                       {:record record
                        :audit [{:node :commit :record record}]})))
      (g/add-node :hold
                   (fn [{:keys [verdict]}]
                     (store/append-ledger! store {:disposition :hold :verdict verdict})
                     {:audit [{:node :hold :verdict verdict}]}))
      (g/set-entry-point :intake)
      (g/add-edge :intake :advise)
      (g/add-edge :advise :govern)
      (g/add-edge :govern :decide)
      (g/add-conditional-edges
       :decide
       (fn [{:keys [disposition]}]
         (case disposition
           :commit :commit
           :request-approval :request-approval
           :hold)))
      (g/add-edge :request-approval :commit)
      (g/set-finish-point :commit)
      (g/set-finish-point :hold)
      (g/compile-graph {:checkpointer checkpointer
                         :interrupt-before #{:request-approval}})))

(defn run-request!
  "Run one operation request to completion or interrupt. `thread-id`
  scopes checkpointing for resume after human approval."
  [graph request context thread-id]
  (g/run* graph {:request request :context context} {:thread-id thread-id}))

(defn approve!
  "Human-in-the-loop resume: the interrupted `:request-approval` node
  advances straight to `:commit` on resume (approval is the act of
  resuming the thread)."
  [graph thread-id]
  (g/run* graph nil {:thread-id thread-id :resume? true}))
