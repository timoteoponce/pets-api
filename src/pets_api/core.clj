(ns pets-api.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-response]]))

;; log requests
(defn wrap-logging [handler]
  (fn [request]
    (log/info (str "Request: "
                   (:request-method request ) " "
                   (:uri request)
                   (when-let [query (:query-string request)]
                     (str "?" query))))
    (handler request)))

;; Define routes
(defroutes app-routes
  (GET "/" [] {:status 200 :body {:message "Helloo World"}}))

;; Wrap the routes with JSON middleware
(def app
  (-> app-routes
      wrap-logging
      wrap-json-response))

;; Main function to start the Jetty server
(defn -main []
  (run-jetty app {:port 3000 :join? false}))

