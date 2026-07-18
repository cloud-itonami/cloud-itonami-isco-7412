(ns elecmech.advisor
  "Electrical Mechanic Advisor — proposing a service scheduling/logistics
  coordination operation (log a service record, schedule a service
  operation, flag a safety concern, coordinate an electrical-parts
  supply order) from a technician roster, service-account registration
  and safety-reporting policy. Swappable mock/llm; the advisor ONLY
  proposes — `elecmech.governor` independently gates every proposal
  and always escalates safety concerns and above-threshold supply
  orders. The advisor never proposes to directly finalize an
  electrical-repair-execution decision (e.g. proceeding with an
  electrical repair), authorize a lockout/tagout clearance, or
  override an electrical safety officer's judgment — those stay
  permanently out of this actor's scope. Modeled on
  cloud-itonami-isco-7127's hvacmech.advisor (closest domain shape —
  same physical-safety-domain, certification-gated technician-safety
  pattern).

  A proposal: {:op :log-service-record|:schedule-service-operation|
               :flag-safety-concern|:coordinate-supply-order
               :effect :propose :technician-id str :service-account-id str
               :cost number :hazard-type kw :task str :stake kw
               :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- rationale-for [op technician-id service-account-id hazard-type]
  (case op
    :log-service-record
    (str "logged service record for technician " technician-id
         " at service account " service-account-id)

    :schedule-service-operation
    (str "scheduled service operation for electrical diagnostic task at service account "
         service-account-id)

    :flag-safety-concern
    (str "flagged " (name (or hazard-type :hazard)) " concern for technician "
         technician-id " at service account " service-account-id
         " — routed for certified electrical safety officer review")

    :coordinate-supply-order
    (str "coordinated supply order for technician " technician-id
         " at service account " service-account-id)

    (str "proposed " (name op) " for technician " technician-id
         " at service account " service-account-id)))

(defn- infer [_store {:keys [op stake technician-id service-account-id cost hazard-type task]
                       :as request}]
  {:op op
   :effect :propose
   :technician-id technician-id
   :service-account-id service-account-id
   :cost cost
   :hazard-type hazard-type
   :task task
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (rationale-for op technician-id service-account-id hazard-type)})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are an electrical mechanics/fitting service scheduling/logistics
   coordination advisor. Given a request, propose an :op (one of
   :log-service-record, :schedule-service-operation,
   :flag-safety-concern, :coordinate-supply-order), the
   :technician-id, :service-account-id, and any
   :cost/:hazard-type/:task fields, an honest :confidence and a
   :stake. Never propose an op outside this closed list, and never
   propose to directly finalize an electrical-repair-execution
   decision (e.g. proceeding with an electrical repair), to authorize
   a lockout/tagout clearance, or to override an electrical safety
   officer's judgment — those are always out of this actor's scope; it
   coordinates service scheduling/logistics only and never performs
   electrical-repair work or authorizes electrical-repair execution
   itself. Safety concerns always require human sign-off regardless of
   confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
