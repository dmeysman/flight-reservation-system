(ns flight-reservation-system.core
  (:require [clojure.string]
            [clojure.pprint]
            [taoensso.timbre :as timbre
              :refer (log  trace  debug  info  warn  error  fatal  report
                      logf tracef debugf infof warnf errorf fatalf reportf
                      spy get-env log-env)]
            [taoensso.timbre.profiling :as profiling
              :refer (pspy pspy* profile defnp p p*)]
            ;[flight-reservation-system.ref-based :as system]))
            ;[flight-reservation-system.atom-based :as system]))
            [flight-reservation-system.agent-based :as system]))
            ;[flight-reservation-system.sequential :as system]))

(defn -main [& args]
  ;(profile :info :main (system/main)))
  (system/main))
