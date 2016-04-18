(defproject flight-reservation-system "0.1.0-SNAPSHOT"
  :description "Flight reservation system for Multicore Programming (Spring 2016) at the Vrije Universiteit Brussel"
  :url "https://github.com/dmeysman/flight-reservation-system"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "4.3.1"]]
  :main ^:skip-aot flight-reservation-system.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
