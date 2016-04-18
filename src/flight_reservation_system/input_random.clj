(ns flight-reservation-system.input-random)

(def flights
  [{:id 0
    :from "BRU" :to "ATL"
    :pricing [[100 15000 0] ; price; # seats available at that price; # seats taken at that price
              [150  5000 0]
              [200  5000 0]
              [300  5000 0]]}
   {:id 1
    :from "BRU" :to "ATL"
    :pricing [[100  5000 0]
              [200 15000 0]
              [320  2000 0]
              [240  3000 0]]}
   {:id 2
    :from "BRU" :to "TXL"
    :pricing [[250 10000 0]
              [300  5000 0]]}
   {:id 3
    :from "BRU" :to "TXL"
    :pricing [[250 10000 0]
              [300  5000 0]]}
   {:id 4
    :from "BRU" :to "LHR"
    :pricing [[250 10000 0]
              [350  5000 0]]}
   {:id 5
    :from "BRU" :to "LGW"
    :pricing [[150 10000 0]
              [300 10000 0]]}
   {:id 6
    :from "BRU" :to "CDG"
    :pricing [[200 15000 0]
              [250  5000 0]
              [300 10000 0]]}
   {:id 7
    :from "BRU" :to "NCE"
    :pricing [[200 15000 0]
              [250  5000 0]
              [300  8000 0]
              [350  2000 0]]}
   {:id 8
    :from "BRU" :to "MAD"
    :pricing [[200 15000 0]
              [250  5000 0]
              [300  8000 0]
              [350  2000 0]]}
   {:id 9
    :from "BRU" :to "LAX"
    :pricing [[250 15000 0]
              [300  5000 0]]}
   {:id 10
    :from "BRU" :to "JFK"
    :pricing [[250 15000 0]
              [300  5000 0]]}
   {:id 11
    :from "BRU" :to "FCO"
    :pricing [[150 15000 0]
              [300  5000 0]]}])

(def customers
  (for [id (range 100000)
        :let [{from :from to :to} (rand-nth flights)]]
    {:id     id
     :from   from
     :to     to
     :seats  (+ (rand-int 4) 1)        ; 1-4
     :budget (+ (rand-int 600) 200)})) ; 200-799

(def NUMBER_OF_CUSTOMERS 100000)
(def TIME_BETWEEN_SALES 250) ; milliseconds
(def TIME_OF_SALES 250)
