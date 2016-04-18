(ns flight-reservation-system.agent-based
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
            [flight-reservation-system.input-random :as input]))

(def ^:const threads 8)

(defnp initialize-flights [initial-flights]
  "Set `flights` agent state to the `initial-flights`."
  (let [grouped-flights (group-by :to initial-flights)]
    (def
      flights
      (zipmap
        (map first grouped-flights)
        (map (comp agent first rest) grouped-flights)))))

(defnp print-flights [flights]
  "Print `flights`."
  (letfn [(pricing->str [pricing]
            (->> pricing
              (map (fn [[p a t]] (clojure.pprint/cl-format nil "$~3d: ~3d ~3d" p a t)))
              (clojure.string/join ", ")))]
    (doseq [{:keys [id from to pricing]} flights]
      (locking *out*
        (println (clojure.pprint/cl-format nil "Flight ~3d from ~a to ~a: ~a"
          id from to (pricing->str pricing)))))))

(defnp update-pricing [flight factor]
  "Updated pricing of `flight` with `factor`."
  (update flight :pricing
    (fn [pricing]
      (map (fn [[p a t]] [(* p factor) a t]) pricing))))

(defnp start-sale []
  "Sale: all flights -20%."
  ;(locking *out*
    (println "Start sale!");)
  (doseq [agent (vals flights)]
    (send agent
      (fn [fs]
        (map (fn [f] (update-pricing f 0.80)) fs)))))

(defnp end-sale []
  "End sale: all flights +25% (inverse of -20%)."
  ;(locking *out*
    (println "End sale!");)
  (doseq [agent (vals flights)]
    (send agent
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

(defnp process-customer [customer]
  (fn [flights]
    (if-let [flight (find-flight flights customer)]
      (let [{updated-flight :flight price :price} (book flight customer)]
        (locking *out*
            (println "Customer" (:id customer) "booked" (:seats customer)
            "seats on flight" (:id updated-flight) "at $" price " (< budget of $"
            (:budget customer) ")."))
        (clojure.walk/postwalk #(if (= (:id %) (:id updated-flight)) updated-flight %) flights))
      (do
        (locking *out*
          (println "Customer" (:id customer) "did not find a flight."))
        flights))))

(defnp process-customers [customers]
  "Process `customers` in parallel"
  (dorun (map #(send (flights (:to %)) (process-customer %)) customers)))

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
  (let [sales (future (sales-process))]
    (initialize-flights input/flights)
    (process-customers input/customers)
    (doseq [agent (vals flights)]
      (await agent))
    (shutdown-agents)
    (swap! finished-processing (fn [value] true))
    @sales
    (println "Flights:")
    (doseq [agent (vals flights)]
      (print-flights @agent))))
