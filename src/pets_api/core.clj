(ns pets-api.core
  (:gen-class)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]))

(defroutes app-routes
  (GET "/" [] {:status 200
               :headers {"Content-Type" "application/json"}
               :body "{\"message\": \" Hello, World!\"}"}))

(defn -main []
  (run-jetty app-routes {:port 3000 :join? false}))
