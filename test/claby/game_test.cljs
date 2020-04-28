(ns claby.game-test
  (:require [clojure.test :refer [testing deftest is are]]
            [clojure.spec.alpha :as s]
            [cljs.spec.gen.alpha :as gen]            
            [claby.game :as g]))

(def test-size 10)
(def test-state (g/create-game test-size))

(deftest move-player-test
  (testing "Moves correctly up, down, right, left on canonical
    board (see create game)"
    (let [test-state (assoc test-state ::g/player-position [1 0])]
      (are [x y] (= x (::g/player-position (g/move-player test-state y)))
        [0 0] :up
        [1 1] :right
        [2 0] :down
        [1 (dec test-size)] :left)))

  (testing "Multiple movement tests"
    (are [x y] (= x (::g/player-position (g/move-player-path test-state y)))
      [0 (- test-size 3)] [:left :left :left]
      [2 2] [:right :down :right :down]
      [0 0] [:left :down :up :right]))

  (testing "Moves correctly when blocked by wall. Here (canonical
  board, position [0 0]) it means up is not possible, all other
  directions are, down then twice up blocks back to initial position."
    (are [x y] (= x (::g/player-position (g/move-player test-state y)))
      [0 0] :up ;; blocked by wall on other side
      [0 1] :right
      [1 0] :down
      [0 (dec test-size)] :left)
    (is (= [0 0]
           (::g/player-position (g/move-player-path test-state [:down :up :up])))))

  (testing "If encircled by walls, can't move"
    (let [test-state
          ;; creating test game with player encircled
          (-> test-state  
              ::g/game-board
              (assoc 0 (assoc ((::g/game-board test-state) 0)
                              1 :wall (dec test-size) :wall))
              (assoc 1 (assoc ((::g/game-board test-state) 1)
                              0 :wall))
              ((fn [x] (hash-map ::g/game-board x ::g/player-position [0 0]))))]
      ;; blocked everywhere
      (are [x y] (= x (::g/player-position (g/move-player test-state y)))
        [0 0] :up 
        [0 0] :right
        [0 0] :down
        [0 0] :left))))
      
