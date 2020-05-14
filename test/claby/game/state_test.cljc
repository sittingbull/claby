(ns claby.game.state-test
  (:require [clojure.test :refer [testing deftest is are]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.test.alpha :as st]
            [claby.utils
             #?(:clj :refer :cljs :refer-macros) [check-all-specs]]
            [claby.game.board :as gb]
            [claby.game.state :as g]))

;;(st/instrument)
;;(check-all-specs claby.game.state)

(def test-size 10)

(def test-state
  "A game with a test board of size 10, last line wall and before last
  line fruits, player at position [0 0]"
  (-> (gb/empty-board test-size)
      (assoc (- test-size 2) (vec (repeat test-size :fruit)))
      (assoc (- test-size 1) (vec (repeat test-size :wall)))
      (g/init-game-state 0)
      (assoc ::g/player-position [0 0])))

(deftest board-spec-test
  (testing "Player should not be able to be on a wall"
    (is (not (s/valid? ::g/game-state
                       (assoc test-state ::g/player-position [9 9]))))))

(deftest get-html-for-state-t
  (testing "Converts appropriately a board to reagent html"
    (is (= [:tbody
            [:tr {:key "claby-0"}
             [:td.empty.enemy-0 {:key "claby-0-0"}]
             [:td.empty {:key "claby-0-1"}]
             [:td.wall {:key "claby-0-2"}]
             [:td.empty {:key "claby-0-3"}]
             [:td.empty {:key "claby-0-4"}]]
            [:tr {:key "claby-1"}
             [:td.empty {:key "claby-1-0"}]
             [:td.fruit {:key "claby-1-1"}]
             [:td.empty.player {:key "claby-1-2"}]
             [:td.empty {:key "claby-1-3"}]
             [:td.empty.enemy-1 {:key "claby-1-4"}]]
            [:tr {:key "claby-2"}
             [:td.empty {:key "claby-2-0"}]
             [:td.empty {:key "claby-2-1"}]
             [:td.wall {:key "claby-2-2"}]
             [:td.empty {:key "claby-2-3"}]
             [:td.empty {:key "claby-2-4"}]]
            [:tr {:key "claby-3"}
             [:td.empty {:key "claby-3-0"}]
             [:td.empty {:key "claby-3-1"}]
             [:td.cheese {:key "claby-3-2"}]
             [:td.cheese {:key "claby-3-3"}]
             [:td.empty {:key "claby-3-4"}]]
            [:tr {:key "claby-4"}
             [:td.empty {:key "claby-4-0"}]
             [:td.empty {:key "claby-4-1"}]
             [:td.empty {:key "claby-4-2"}]
             [:td.empty {:key "claby-4-3"}]
             [:td.empty {:key "claby-4-4"}]]]

           (g/get-html-for-state
            {::g/status :active
             ::g/score 10
             ::g/enemy-positions [[0 0] [1 4]]
             ::gb/game-board [[:empty :empty :wall :empty :empty]
                             [:empty :fruit :empty :empty :empty]
                             [:empty :empty :wall :empty :empty]
                             [:empty :empty :cheese :cheese :empty]
                             [:empty :empty :empty :empty :empty]]
             ::g/player-position [1 2]})))))

(deftest enjoyable-game-test
  (let [fully-accessible-board-1                     
        [[:wall :empty :fruit :wall :wall]
         [:wall :fruit :wall :empty :wall]
         [:wall :empty :empty :empty :empty]
         [:wall :wall :cheese :wall :wall]
         [:wall :wall :fruit :wall :wall]]
        fully-accessible-board-2
        [[:empty :empty :wall :empty :empty]
         [:empty :fruit :empty :empty :empty]
         [:empty :empty :wall :wall :wall]
         [:wall :empty :cheese :cheese :fruit]
         [:empty :empty :empty :wall :empty]]
        board-with-locked-player-or-enemies
        [[:empty :empty :wall :wall :empty]
         [:empty :wall :empty :empty :wall]
         [:empty :empty :wall :wall :wall]
         [:fruit :empty :cheese :cheese :fruit]
         [:empty :empty :empty :wall :empty]]
        board-with-locked-fruit
        [[:empty :empty :wall :empty :empty]
         [:empty :fruit :empty :empty :empty]
         [:empty :empty :wall :wall :wall]
         [:wall :empty :cheese :cheese :fruit]
         [:empty :empty :empty :wall :wall]]
        player-and-enemy-positions
        [[[0 1] [2 1] [1 3]]
         [[1 3] [0 1] [2 1]]
         [[2 1] [1 3] [0 1]]]
        create-state-from-positions
        (fn [b pos]
          (-> (g/init-game-state b 0)
              (assoc ::g/player-position (first pos))
              (assoc ::g/enemy-positions (vec (rest pos)))))]
        
    (is (every? g/enjoyable-game?
                (map #(create-state-from-positions (first %) (second %))
                     (for [board [fully-accessible-board-1
                                  fully-accessible-board-2]
                           pos player-and-enemy-positions]
                       [board pos]))))
    (is (not-any? g/enjoyable-game?
                (map #(create-state-from-positions (first %) (second %))
                     (for [board [board-with-locked-player-or-enemies
                                  board-with-locked-fruit]
                           pos player-and-enemy-positions]
                       [board pos]))))
    (is (g/enjoyable-game? (create-state-from-positions
                          board-with-locked-player-or-enemies
                          [[0 0] [0 1] [1 0]])))
    (is (not (g/enjoyable-game? (create-state-from-positions
                               board-with-locked-fruit
                               [[0 0] [0 1] [1 0]]))))))    
    
