(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'pets-api)
(def version "0.1.0")
(def class-dir "target/classes")
; (def jar-file (format "target/%s-%s.jar" (name lib) version))
(def jar-file (format "target/app.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis (b/create-basis {})
                  :src-dirs ["src" "resources"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file jar-file
           :basis (b/create-basis {})
           :main 'pets-api.core}))
