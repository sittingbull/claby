(ns claby.game.generation-test
  (:require [clojure.test :refer [testing deftest is are]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as st]
            [claby.utils.testing
             #?(:clj :refer :cljs :refer-macros) [check-all-specs]]
            [claby.game.state-test :refer [test-size test-state]]
            [claby.game.board-test :refer [small-test-board]]
            [claby.game.board :as gb]
            [claby.game.generation :as gg]
            [clojure.data.generators :as g]
            [claby.utils.testing :as u]))

(st/instrument)
(check-all-specs claby.game.generation)

(deftest generate-wall-randomly
  (testing "that walls are generated somewhat randomly")
  (let [[wall1 wall2 wall3]
        (repeatedly 3 #(gg/generate-wall test-size (- test-size 3)))]
    
    (are [x y] (and (not= (x 0) (y 0)) (not= (x 1) (y 1)))
      wall1 wall2
      wall1 wall3
      wall2 wall3)))

(deftest add-wall-correctly
  (let [test-board (test-state ::gb/game-board)
        wall [[1 1] [:right :right :right :up :up]]
        walled-board (gg/add-wall test-board wall)]
    (is (every? #(= (test-board %) (walled-board %)) (range 2 test-size)))
    (is (every? #(= :wall %) (subvec (walled-board 1) 1 5)))
    (is (= :wall (get-in walled-board [0 4])))))

(deftest sum-of-densities-basic-test
  (is (= 11 (gg/sum-of-densities (test-state ::gb/game-board))))
  (is (= 16 (gg/sum-of-densities small-test-board))))

(deftest create-nice-board-test
  (testing "Appropriate densities in board"
    (with-redefs [gg/add-wall (u/count-calls gg/add-wall)]
      (let [board (gg/create-nice-board 6 {::gg/density-map {:fruit 5 :cheese 10}})]
        (is (gg/valid-density board :fruit 5))
        (is (gg/valid-density board :cheese 10))
        (is (= 3 ((:call-count (meta gg/add-wall))))))
      
      (let [board (gg/create-nice-board 8 {::gg/density-map {:fruit 5 :cheese 10}
                                           ::gg/wall-density 0})]
        (is (gg/valid-density board :fruit 5))
        (is (gg/valid-density board :cheese 10))
        (is (= 0 ((:call-count (meta gg/add-wall))))))
      
      (let [board (gg/create-nice-board 13 {::gg/density-map {:fruit 7 :cheese 4}
                                            ::gg/wall-density 25})]
        (is (gg/valid-density board :fruit 7))
        (is (gg/valid-density board :cheese 4))
        (is (= 3 ((:call-count (meta gg/add-wall)))))))))

(deftest valid-density-issue
  (let [board
        [[:empty :cheese :empty :wall :fruit :fruit :empty :empty]
         [:empty :fruit :empty :fruit :cheese :fruit :empty :empty]
         [:empty :fruit :empty :fruit :empty :empty :wall :empty]
         [:empty :empty :wall :fruit :empty :empty :empty :empty]
         [:empty :empty :fruit :cheese :cheese :empty :wall :empty]
         [:empty :wall :fruit :empty :empty :cheese :empty :empty]
         [:empty :empty :empty :empty :empty :empty :wall :empty]
         [:empty :cheese :empty :empty :empty :fruit :cheese :empty]]
        desired-density 18
        element :fruit]
    (is (= (-> board gb/board-stats :density :fruit) 18))
    (is (gg/valid-density board :fruit 18))))

(deftest correctly-seeded-generation
  (testing "With a given seed for the random number gen, should always
  return the same nice board"
    (let [gen-same-board
          #(binding [g/*rnd* (java.util.Random. 20)]
             (gg/create-nice-game 10 {::gg/density-map {:fruit 10}}))]
      (is (every? #(= % (gen-same-board)) (repeatedly 10 gen-same-board))))))
