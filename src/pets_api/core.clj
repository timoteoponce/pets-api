(ns pets-api.core
  (:gen-class)
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.util.response :refer [response status resource-response]]
            [clojure.java.io :as io])
  (:import [org.sqlite SQLiteException]))

(def db-spec
  {:dbtype "sqlite"
   :dbname (or (System/getenv "DB_PATH") "database/pets.db")})

(defn init-db []
  (try
    (let [file (java.io.File. (get db-spec :dbname))]
      (log/info (str ":db-path " (.getAbsolutePath file))))
    (jdbc/db-do-commands db-spec
                         (jdbc/create-table-ddl :pets
                                                [[:id :integer :primary :key :autoincrement]
                                                 [:name :text :not :null :unique]]))
    (log/info "Created Pets table")
    (catch Exception e
      (if (re-find #"table.*already exists" (.getMessage e))
        (log/info "Pets table already exists")
        (do
          (log/error e "Failed to create Pets table")
          (throw e))))))

(defn name-exists? [name id]
  (if (some? id)
    (seq (jdbc/query db-spec ["SELECT 1 FROM pets WHERE name = ? AND id <> ?" name id]))
    (seq (jdbc/query db-spec ["SELECT 1 FROM pets WHERE name = ? " name]))))

(defn create-pet [name]
  (if (name-exists? name nil)
    (do
      (log/warn (str "Duplicate name not allowed: " name))
      {:error "Pet name must be unique"})
    (try
      (jdbc/with-db-transaction [tx db-spec]
        (jdbc/insert! tx :pets {:name name} {:return-keys true})
        (let [pet (first (jdbc/query tx ["SELECT id, name FROM pets WHERE name = ?" name]))]
          (log/info (str "Created pet: " name))
          pet))
      (catch Exception e
        (log/error e (str "Failed to create pet: " name))
        (throw e)))))

(defn get-all-pets []
  (jdbc/query db-spec ["SELECT id, name FROM pets"]))

(defn get-pet [id]
  (first (jdbc/query db-spec ["SELECT id, name FROM pets WHERE id = ?" id])))

(defn update-pet [id name]
  (let [id (Long/parseLong id)] ; Ensure id is a number
    (if (name-exists? name id)
      (do
        (log/warn (str "Duplicate name not allowed: " name))
        {:error "Pet name must be unique"})
      (try
        (let [result (jdbc/update! db-spec :pets {:name name} ["id = ?" id])]
          (if (pos? (first result))
            (do
              (log/info (str "Updated pet id: " id " to name: " name))
              {:id id :name name})
            (do
              (log/warn (str "Pet not found: id=" id))
              {:error "Pet not found"})))
        (catch Exception e
          (log/error e (str "Failed to update pet id: " id))
          (throw e))))))

(defn delete-pet [id]
  (let [result (jdbc/delete! db-spec :pets ["id = ?" id])]
    (if (pos? (first result))
      (do
        (log/info (str "Deleted pet id: " id))
        {:status "success"})
      (do
        (log/warn (str "Pet not found: id=" id))
        {:error "Pet not found"}))))

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
  (GET "/" [] 
       (let [resource-path "api-docs.html"
          resource (io/resource resource-path)]
      (if resource
        (do
          (log/debug (str "Serving API docs from: " (.toString resource)))
          (-> (resource-response resource-path)
              (assoc-in [:headers "Content-Type"] "text/html")))
        (do
          (log/error (str "API documentation file not found: " resource-path)
                     "Classpath:" (vec (.getURLs (java.net.URLClassLoader/getSystemClassLoader))))
          (-> (response {:error "API documentation not found"})
              (status 404))))))

  (GET "/pets" []
    (response (get-all-pets)))
  (GET "/pets/:id" [id]
    (if-let [pet (get-pet (Long/parseLong id))]
      (response pet)
      (do
        (log/warn (str "Pet not found: id=" id))
        (-> (response {:error "Pet not found"})
            (status 404)))))
  (POST "/pets" req
    (let [name (get-in req [:body :name])]
      (if name
        (let [result (create-pet name)]
          (if (:error result)
            (-> (response result)
                (status 400))
            (response result)))
        (do
          (log/warn "Missing name in request body")
          (-> (response {:error "Name is required"})
              (status 400))))))
  (PUT "/pets/:id" [id :as req]
    (let [name (get-in req [:body :name])]
      (if name
        (let [result (update-pet id name)]
          (if (:error result)
            (-> (response result)
                (status 400))
            (response result)))
        (do
          (log/warn "Missing name in request body")
          (-> (response {:error "Name is required"})
              (status 400))))))
  (DELETE "/pets/:id" [id]
    (let [result (delete-pet (Long/parseLong id))]
      (if (:error result)
        (-> (response result)
            (status 404))
        (response result)))))

;; Wrap the routes with JSON middleware
(def app
  (-> app-routes
      wrap-logging
      (wrap-json-body {:keywords? true})
      wrap-json-response))

;; Main function to start the Jetty server
(defn -main []
  (init-db)
  (run-jetty app {:port 3000 :host "0.0.0.0" :join? false}))

