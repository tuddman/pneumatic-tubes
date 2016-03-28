(ns pneumatic-tubes.core-test
  (:require [clojure.test :refer :all]
            [pneumatic-tubes.core :refer :all]))

(defn- noop [])

(deftest tube-registry-test

  (testing "Add tube should returns a tube id"
    (is (not (nil? (add-tube! noop)))))

  (testing "Should get added tube by id"
    (is (not (nil? (get-tube (add-tube! noop))))))

  (testing "Should find tube by id"
    (is (= 1 (count (find-tubes (add-tube! noop))))))

  (testing "Should find tube by map with key :tube/id"
    (is (= 1 (count (find-tubes {:tube/id (add-tube! noop)})))))

  (testing "Should find tube by custom data"
    (add-tube! noop {:label "value1"})
    (is (= 1 (count (find-tubes #(= (:label %) "value1"))))))

  (testing "Can NOT set custom :tube/id"
    (let [tube-id (add-tube! noop {:tube/id "my-id"})]
      (is (= {:tube/id tube-id} (get-tube tube-id)))))

  (testing "Should NOT find tube by incorrect criteria"
    (add-tube! noop {:label "value2"})
    (is (= 0 (count (find-tubes #(= (:label %) "no-such-value"))))))

  (testing "Should update tube data"
    (update-tube-data! (add-tube! noop) {:label "value3"})
    (is (= 1 (count (find-tubes #(= (:label %) "value3"))))))

  (testing "Should NOT update tube data if tube does not exist"
    (update-tube-data! "not-existing-id" {:label "value4"})
    (is (nil? (get-tube "not-existing-id"))))

  (testing "Should NOT update tube-id "
    (let [tube-id (update-tube-data! (add-tube! noop) {:tube/id "my-id"})]
      (is (= {:tube/id tube-id} (get-tube tube-id)))))

  (testing "Should remove tube"
    (is (nil? (get-tube (rm-tube! (add-tube! noop)))))))

