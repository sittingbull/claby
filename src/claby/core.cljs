(ns ^:figwheel-hooks claby.core
  "Entry point to run the Claby game.

  See game.cljs for more about the game."
  (:require
   [goog.dom :as gdom]
   [clojure.test.check]
   [clojure.test.check.properties]
   [cljs.spec.alpha :as s]
   [cljs.spec.gen.alpha :as gen]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :refer [render]]
   [claby.game.board :as gb]
   [claby.game.state :as gs]
   [claby.game.events :as ge]
   [claby.game.generation :as gg]))

(defonce game-size 27)

(defonce levels
  [{:message "Lapinette enceinte doit manger un maximum de fraises"
    ::gg/density-map {:fruit 5
                      :cheese 0}
    :enemies [:mouse :drink :virus]}
   {:message "Attention au fromage non-pasteurisé !"
    ::gg/density-map {:fruit 5
                      :cheese 3}
    :message-color "darkgoldenrod"}
   ])

(defonce game-state (atom {}))
(defonce level (atom 0))

;;; Player movement
;;;;;;

;;; Scroll if player moves too far up / down

(s/fdef board-scroll
  :args (s/cat :state ::gs/game-state)
  :ret (s/or :double (s/double-in 0 1) :nil nil?))

(defn board-scroll
  "Returns the value of scroll needed so that player remains visible, as
  a fraction of the window height. If player is on top third of board,
  scroll to 0, on bottom-third, scroll to mid-page."
  [{:keys [::gs/player-position ::gb/game-board], :as state}]
  (let [size (count game-board)]
    (cond
      (< (player-position 0) (* size 0.4)) 0
      (> (player-position 0) (* size 0.7)) 0.5)))

(defn move-player
  "Moves player on the board by changing player-position"
  [e]
  (let [direction (case (.-key e)
                    ("ArrowUp" "e" "E") :up
                    ("ArrowDown" "d" "D") :down
                    ("ArrowLeft" "s" "S") :left
                    ("ArrowRight" "f" "F") :right
                    :no-movement)]
    (when (not= direction :no-movement)
      (.preventDefault e)
      (swap! game-state ge/move-player direction)
      #_(when-let [scroll-value (board-scroll @game-state)]
        (.scroll js/window 0 (* scroll-value (.-innerHeight js/window)))))))

  
;;;
;;; Component & app rendering
;;;
(defonce jq (js* "$"))
(defonce gameMusic (js/Audio. "neverever.mp3"))
(defonce scoreSound (js/Audio. "coin.wav"))
(defonce sounds
  {:over (js/Audio. "over.wav")
   :won (js/Audio. "won.mp3")
   :nextlevel (js/Audio. "nextlevel.wav")})

(set! (.-loop gameMusic) true)

(defn add-enemies-style
  [enemies]
  (.remove (jq "#app style"))
  (doall (map-indexed  
          #(.append (jq "#app")
                    (str "<style>td.enemy-" %1
                         " {background-image: url(../img/" (name %2)
                         ".gif)}</style>"))
          enemies)))

(defn start-game
  ([elt-to-fade callback]
   (.addEventListener js/window "keydown" move-player)
   (add-enemies-style (get-in levels [@level :enemies]))
   (swap! game-state #(gs/init-game-state
                       (gg/create-nice-board game-size (levels @level))
                       (count (get-in levels [@level :enemies]))))
   (-> (.play gameMusic))
   (.fadeTo (jq "#h") 1000 1 callback)
   (.fadeOut (jq elt-to-fade) 1000))
   
  
  ([elt-to-fade]
   (start-game elt-to-fade nil)))
  

(defn animate-game
  ([status callback in-between-callback]
   (.scroll js/window 0 0)
   (.removeEventListener js/window "keydown" move-player) ;; freeze player
   (.pause gameMusic)
   (set! (.-onended (sounds status)) callback)
   (.play (sounds status))
   (.fadeTo (jq "#h") 500 0)
   (.fadeIn (jq (str ".game-" (name status))) 1000 in-between-callback))

  ([status]
   (animate-game status nil nil)))

(defn between-levels []
  (.addClass (jq "#h h2.subtitle") "initial")
  (.css (jq "#h h2.subtitle span") "color" (get-in levels [@level :message-color])))
  
(defn next-level-callback []
  (.animate (jq "#h h2.subtitle") (clj->js {:top "0em" :font-size "1.2em"}) 2500))

(defn final-animation [i]
  (cond

    (< i 6)
    (do (.remove (jq ".game-won img"))
        (.append (jq ".game-won") (str "<img src=\"img/ending/" i ".gif\">"))
        (-> (jq ".game-won img")
            (.hide)
            (.fadeIn 500)
            (.delay 4300)
            (.fadeOut 500 #(final-animation (inc i)))))

    (= i 6)
    (do (.remove (jq ".game-won img"))
        (.append (jq ".game-won") "<span><p>BIEN<br/>OUEJ!</p></span>")
        (-> (jq ".game-won span p")
            (.css "font-size" "0.1em")
            (.animate
             (clj->js {:font-size "3em"})
             (clj->js
              {:duration 1000
               :step (fn [now fx]
                       (this-as this
                         (.css (jq this) "transform" (str "rotate(" (* now 360) "deg)"))))}))
            (.delay 2000)
            (.fadeOut 500 #(final-animation 7))))

    (= i 7)
    (do (.append (jq ".game-won") (str "<img src=\"img/ending/6.gif\">"))
        (.append (jq ".game-won") "<p>The end.</p>")
        (.append (jq ".game-won") "<p class=\"slide9\">Hiding text</p>")
        (-> (jq ".game-won img")
            (.hide)
            (.fadeIn 500 #(final-animation 8))))

    (= i 8)
    (do (.animate (jq ".game-won p.slide9") (clj->js {:left "30em"}) 2000))))
    



(defn game-transition [status]
  (cond
    (and (= status :won) (< (inc @level) (count levels)))
    (do (animate-game :nextlevel
                      (fn [] (start-game ".game-nextlevel" next-level-callback))
                      between-levels)
        (swap! level inc))
    
    (= status :won)
    (animate-game status nil #(final-animation 0))
  
    (= status :over)
    (animate-game status))
  [:div])

(defn get-app-element []
  (gdom/getElement "app"))

(defn show-score
  [score]
  (if (pos? score) (.play scoreSound))
  [:div.score
   [:span (str "Score: " score)]
   [:br]
   [:span (str "Level: " (inc @level))]])

(defn claby []
  (if (re-find #"Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini"
               (.-userAgent js/navigator))
    [:h2.subtitle "Le jeu est prévu pour fonctionner sur ordinateur (mac/pc)"]

    [:div#lapyrinthe.row.justify-content-md-center
     [:h2.subtitle [:span (get-in levels [@level :message])]]
     [:div.col.col-lg-2]
     [:div.col-md-auto
      [show-score (@game-state ::gs/score)]
      [:table (gs/get-html-for-state @game-state)]]
   [:div.col.col-lg-2]
   [game-transition (@game-state ::gs/status)]]))




(defn mount [el]
  (.click (jq ".game-over button") #(start-game ".game-over"))
  #_(do (comment "Test ending")
      (.hide (jq "#surprise"))
      (.show (jq ".game-won"))
      (final-animation 6))
  (.click (jq "#surprise img")
          (fn []
            (.click (jq "#surprise img") nil)
            (start-game "#surprise" #(.fadeOut (jq "#h h1") 2000))))
  (render [claby] el))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

(mount-app-element)


;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
