(ns flight-reservation-system.atom-based
  (:require [clojure.string]
            [clojure.pprint]
            [clojure.walk]
            [taoensso.timbre :as timbre
              :refer (log  trace  debug  info  warn  error  fatal  report
                      logf tracef debugf infof warnf errorf fatalf reportf
                      spy get-env log-env)]
            [taoensso.timbre.profiling :as profiling
              :refer (pspy pspy* profile defnp p p*)]
            ;[flight-reservation-system.input-simple :as input]))
            [flight-reservation-system.input-uniform :as input]))

(def printer
 (agent nil))

(defnp initialize-flights [initial-flights]
  "Set `flights` agent state to the `initial-flights`."
  (let [grouped-flights (group-by :to initial-flights)]
    (def
      flights
      (zipmap
        (map first grouped-flights)
        (map (comp atom first rest) grouped-flights)))))

(defnp print-flights [flights]
  "Print `flights`."
  (letfn [(pricing->str [pricing]
            (->> pricing
              (map (fn [[p a t]] (clojure.pprint/cl-format nil "$~3d: ~3d ~3d" p a t)))
              (clojure.string/join ", ")))]
    (doseq [{:keys [id from to pricing]} flights]
        (println (clojure.pprint/cl-format nil "Flight ~3d from ~a to ~a: ~a"
          id from to (pricing->str pricing))))))

(defnp update-pricing [flight factor]
  "Updated pricing of `flight` with `factor`."
  (update flight :pricing
    (fn [pricing]
      (map (fn [[p a t]] [(* p factor) a t]) pricing))))

(defnp start-sale []
  "Sale: all flights -20%."
  ;(send-off printer
  ;        (fn [state]
  ;          (println "Start sale!")))
  (doseq [atom (vals flights)]
    (swap! atom
      (fn [fs]
        (map (fn [f] (update-pricing f 0.80)) fs)))))

(defnp end-sale []
  "End sale: all flights +25% (inverse of -20%)."
  ;(send-off printer
  ;        (fn [state]
  ;          (println "End sale!")))
  (doseq [atom (vals flights)]
    (swap! atom
      (fn [fs]
        (map (fn [f] (update-pricing f 1.25)) fs)))))

(defnp sort-pricing [pricing]
  "Sort `pricing` from lowest to highest price."
  (sort-by first pricing))

(defnp filter-pricing-with-n-seats [pricing seats]
  "Get `pricing` for which there are at least `seats` empty seats available."
  (filter #(>= (second %) seats) pricing))

(defnp lowest-available-price [flight seats]
  "Returns the lowest price in `flight` for which at least `seats` empty seats
  are available, or nil if none found."
  (-> (:pricing flight)                 ; [[price available taken]]
    (filter-pricing-with-n-seats seats)
    (sort-pricing)
    (first)                             ; [price available taken]
    (first)))                           ; price

(defnp find-flight [flights customer]
  "Find a flight in `flights` that is on the route and within the budget of
  `customer`. If a flight was found it is returned, else returns nil."
  (let [{:keys [id from to seats budget]}
          customer
        flights-on-route
          (filter #(and (= (:from %) from) (= (:to %) to)) flights)
        flights-in-budget
          (filter
            (fn [f]
              (let [lowest-price (lowest-available-price f seats)]
                (and (some? lowest-price) (<= lowest-price budget))))
          flights-on-route)]
    (first flights-in-budget)))

(defnp book [flight customer]
  "Updates `flight` to book `customer`'s seats, returning
  {:flight updated-flight :price price}."
  (let [seats        (:seats customer)
        lowest-price (lowest-available-price flight seats)
        new-pricing  (for [[p a t] (:pricing flight)]
                       (if (= p lowest-price)
                         [p (- a seats) (+ t seats)]
                         [p a t]))]
    {:flight (assoc flight :pricing new-pricing)
     :price  lowest-price}))

(def finished-processing
  "Set to true once all customers have been processed, so that sales process
  can end."
  (atom false))

(defnp process-customer [flights customer]
  (swap! flights
    (fn [flights-state]
      (if-let [flight (find-flight flights-state customer)]
        (let [{updated-flight :flight price :price} (book flight customer)]
  ;        (send-off printer
  ;         (fn [state]
  ;           (println "Customer" (:id customer) "booked" (:seats customer)
  ;           "seats on flight" (:id updated-flight) "at $" price " (< budget of $"
  ;           (:budget customer) ").")))
          (clojure.walk/postwalk #(if (= (:id %) (:id updated-flight)) updated-flight %) flights-state))
        (do
  ;        (send-off printer
  ;         (fn [state]
  ;           (println "Customer" (:id customer) "did not find a flight.")))
          flights-state)))))

(defnp queue-customers-by-key [customers-by-key]
  (let [key (first customers-by-key)
        customers (first (rest customers-by-key))]
    (doseq [customer customers]
      (process-customer (flights key) customer))))

(defnp process-customers [customers]
  (let [queues (doall (map #(future (queue-customers-by-key %)) (group-by :to customers)))]
    (dorun (map deref queues))
    (reset! finished-processing true)))

(defnp sales-process []
  "The sales process starts and ends sales periods, until `finished-processing`
  is the total amount of customers."
  (loop []
    (Thread/sleep input/TIME_BETWEEN_SALES)
    (start-sale)
    (Thread/sleep input/TIME_OF_SALES)
    (end-sale)
    (if (not @finished-processing)
      (recur))))

(defnp main []
  (initialize-flights input/flights)
  (let [f1 (future (sales-process))]
    (process-customers input/customers)
    @f1
    (shutdown-agents))
  (println "Flights:")
  (doseq [partial-flights (vals flights)]
    (print-flights @partial-flights)))
