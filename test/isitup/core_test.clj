(ns isitup.core-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clj-http.client :as client]
            [isitup.core :as isup]
            [isitup.cli :as cli]))

(def mock-status {:domain "google.com"
                  :port 80
                  :status_code 1
                  :response_ip "127.0.0.1"
                  :response_code 200
                  :response_time 0.038})

(deftest api-calls
  (testing "HTTP requests"
    (with-redefs
      [isup/run-status* (fn [domain] (assoc mock-status :domain domain))]
      (let [res (isup/run-status "google.com")]
        (is (map? res))
        (is (= (:domain res) "google.com"))))
    (with-redefs
      [client/get
        (fn [domain] {:body "{\n    \"domain\": \"google.com\",\n
                            \"port\": 80,\n    \"status_code\": 1,\n
                            \"response_ip\": \"127.0.0.1\",\n
                            \"response_code\": 200,\n
                            \"response_time\": 0.038\n}"})]
      (let [res (#'isup/run-status* "google.com")]
        (is (= (assoc mock-status :domain "google.com") res)))
      (let [res (isup/run-status "google.com")]
        (is (= (assoc mock-status :domain "google.com") res)))
      (is (thrown-with-msg? RuntimeException
                            #"Expected java.lang.String"
                            (isup/run-status nil))))
    (with-redefs
      [client/get
        (fn [domain] (throw (ex-info "clj-http: status 500" {:status 500})))]
      (is (thrown-with-msg? RuntimeException
                            #"API unreachable"
                            (#'isup/run-status* "google.com"))))))

(deftest cli
  (testing "API responses"
    (let [website "google.com"
          response (assoc mock-status :domain website)]
      (are [res output] (= (#'cli/get-output res) output)
        response (str "✔ Up: " website)
        (assoc response :status_code 2) (str "✖ Down: " website)
        (assoc response :status_code 3) (str "⚠ Invalid Domain: " website)))))
