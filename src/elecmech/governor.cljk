(ns elecmech.governor
  "ElecMechGovernor — the independent safety/scope layer gating every
  service scheduling/logistics proposal an advisor may make for an
  electrical mechanic/fitter crew. The governor never dispatches
  hardware itself, never performs electrical-repair work, and never
  finalizes an electrical-repair-execution decision (e.g. deciding to
  proceed with a specific electrical repair), authorizes a
  lockout/tagout clearance, or overrides an electrical safety
  officer's judgment — those are permanently out of this actor's scope
  and remain a certified electrical safety officer's exclusive
  judgment (README's 'Robotics premise': this actor coordinates
  SERVICE SCHEDULING/LOGISTICS ONLY — it never performs electrical
  work itself). Modeled on cloud-itonami-isco-7127's hvacmech.governor
  (closest domain shape — same physical-safety-domain,
  certification-gated technician-safety pattern — itself modeled on
  cloud-itonami-isco-7111's housebuilder.governor,
  cloud-itonami-isco-3313's accountingsupport.governor and
  cloud-itonami-isco-9311's mininglabor.governor for the
  physical-safety-domain shape).

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. technician provenance — the technician must be independently
                                verified/registered (including
                                certification status) before any
                                action.
    2. service-account provenance — the service account must be
                                independently verified/registered
                                before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never performs electrical-repair work
                                itself; it only gates what the advisor
                                may coordinate).
    4. closed op-allowlist    — only :log-service-record,
                                :schedule-service-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to directly finalize an
                                electrical-repair-execution decision
                                (e.g. deciding to proceed with a
                                specific electrical repair), to
                                authorize or finalize a lockout/tagout
                                clearance decision, or to override or
                                bypass an electrical safety officer's
                                judgment, is a hard, permanent block
                                (checked both against the proposed :op
                                and, defense-in-depth, against the
                                proposal's :rationale text — matched as
                                full finalization/execution ACTION
                                phrases such as \"proceed with the
                                electrical repair\" / \"authorize the
                                lockout/tagout clearance\" / \"override
                                the electrical safety officer's
                                judgment\", never as bare nouns like
                                \"electrical\", \"lockout\", \"tagout\"
                                or \"safety officer\", so the check can
                                never self-trip on the advisor's own
                                routine rationale text, e.g. \"logged
                                service record for technician …\" or
                                \"scheduled service operation for
                                electrical diagnostic task …\" or
                                \"…routed for certified electrical
                                safety officer review\" — all three
                                legitimately contain those bare nouns
                                but none is a finalization action, and
                                all are exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (an arc-flash-risk/lockout-tagout-status/
                                equipment-hazard concern always
                                escalates to a human, never
                                auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`)."
  (:require [kotoba.lang.text :as str]
            [elecmech.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-service-record :schedule-service-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops`
;; above, so they are already refused by the closed-allowlist check
;; below; they are named again here — as explicit finalization/
;; execution ACTIONS, never bare nouns — so a future allowlist edit
;; cannot silently re-open this specific out-of-scope path without
;; also touching this list.
(def ^:private scope-excluded-ops
  #{:finalize-electrical-repair-execution-decision
    :authorize-electrical-repair-execution
    :proceed-with-electrical-repair
    :authorize-lockout-tagout-clearance
    :finalize-lockout-tagout-clearance-decision
    :override-electrical-safety-officer-judgment
    :bypass-electrical-safety-officer-judgment})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("electrical", "lockout", "tagout", "safety officer", "repair") —
;; so this can never match inside the mock advisor's own default
;; rationale text (which legitimately contains those bare nouns, e.g.
;; "electrical diagnostic task" / "certified electrical safety officer
;; review"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["proceed with the electrical repair"
   "finalize the electrical-repair execution decision"
   "finalize the electrical repair execution decision"
   "authorize the electrical-repair execution"
   "authorize the electrical repair execution"
   "authorize the lockout/tagout clearance"
   "authorize the lockout tagout clearance"
   "finalize the lockout/tagout clearance decision"
   "finalize the lockout tagout clearance decision"
   "override the electrical safety officer's judgment"
   "override the electrical safety officer judgment"
   "bypass the electrical safety officer's judgment"
   "bypass the electrical safety officer judgment"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal technician-record service-account-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? technician-record)
      (conj {:rule :no-technician
             :detail "未登録 technician への提案は不可（technician record は独立して検証・登録済み — certification status を含む — でなければならない）"})

      (nil? service-account-record)
      (conj {:rule :no-service-account
             :detail "未登録 service account への提案は不可（service account record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は electrical-repair work を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "電気修理実行判断の確定・lockout/tagout clearance の許可/確定・electrical safety officer 判断の上書き/回避は、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `elecmech.store/Store`. Pure — never mutates
  the store, never dispatches a service operation."
  [request _context proposal store]
  (let [technician-record (store/technician store (:technician-id request))
        service-account-record (some->> (:service-account-id proposal) (store/service-account store))
        hard (hard-violations proposal technician-record service-account-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
