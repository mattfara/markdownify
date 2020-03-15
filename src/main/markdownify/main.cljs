(ns markdownify.main
 (:require [reagent.dom :as reagent-dom]
           [reagent.core :as reagent-core]
           ["showdown" :as showdown])) 
;;despite tutorial, in reagent 0.10.0, must use .dom for render, not .core
;;since we're calling showdown directly from package.json, we use the string
  ;;contrasted with reagent, which is a dependency of shadow-cljs


(defonce flash-message (reagent-core/atom nil))
;;reagent atoms are enough for a small app like this to manage state
;;more complex apps benefit from more structure, provided by something like 'reframe'
;;

(defonce flash-timeout (reagent-core/atom nil))

(defn flash
  ([text]
   (flash text 3000))
  ([text ms]
   (js/clearTimeout @flash-timeout)
   (reset! flash-message text)
   (reset! flash-timeout
           (js/setTimeout #(reset! flash-message nil) ms))))

(defonce text-state (reagent-core/atom {:format :md
                                        :value ""}))

(defonce showdown-converter (showdown/Converter.))
;;creates a re-used instance of the showdown converter (note . after class name means "new")
;;there are additional settings you can explore with markdown

(defn md->html [md]
  (.makeHtml showdown-converter md))
;;takes the string and makes html of it using the converter

(defn html->md [html]
  (.makeMarkdown showdown-converter html))
;;notes on syntax: .x means calling fn written in other language
  ;;

(defn ->md [{:keys [format value]}]
  (case format 
    :md value
    :html (html->md value)))

(defn ->html [{:keys [format value]}]
  (case format
    :html value
    :md (md->html value)))


(defn copy-to-clipboard [s]
  (let [el (.createElement js/document "textarea")
        selected (when (pos? (-> js/document .getSelection .-rangeCount ))
                   (-> js/document .getSelection (.getRangeAt 0)))]
    (set! (.-value el) s)
    (.setAttribute el "readonly" "")
    (set! (-> el .-style .-position) "absolute")
    (set! (-> el .-style .-left) "-9999px")
    (-> js/document .-body (.appendChild el))
    (.select el)
    (.execCommand js/document "copy")
    (-> js/document .-body (.removeChild el))
    (when selected
      (-> js/document .getSelection .removeAllRanges)
      (-> js/document .getSelection (.addRange selected)))))
;;this is found online in JS, then converted to CLJS


(defn app []
  [:div {:style {:position :relative}}
   
   [:div 
    {:id "successful-copy" 
     :style {:position :absolute
             :margin :auto
             :left 0
             :right 0
             :text-align :center
             :max-width 200
             :padding "2em"
             :background-color "yellow"
             :z-index 100
             :border-radius 10
             :border-bottom-right-radius 10
             :transform (if @flash-message
                          "scaleY(1)"
                          "scaleY(0)")
             :transition "transform 0.2s ease-out"}}
    @flash-message]
   [:h1 "Markdownify"]
   
   [:div
    {:style {:display :flex}}
    [:div
     {:style {:flex "1"}}
     [:h2 "Markdown"]
     [:textarea
      {:on-change (fn [t] 
                    (reset! text-state {:format :md 
                                        :value (-> t .-target .-value)}))
       ;;on change, reset the atom to be whatever is typed in
       :value (->md @text-state)
       ;;making the value of the text area be "markdown" locks the atom to the element
       :style {:resize "none"
               :height "500px"
               :width "100%"}}]
     [:button 
      {:on-click (fn [] 
                   (copy-to-clipboard (->md @text-state))
                   (flash "Markdown copied to clipboard!")) ;;@ is how atoms are referenced
       :style {:background-color :green
               :border-radius 10
               :padding "1em"
               :color :white}} 
      "Copy Markdown"]]
    
    [:div
     {:style {:flex "1"}}
     [:h2 "HTML"]
     [:textarea
      {:on-change (fn [t]
                    (reset! text-state {:format :html
                                        :value (-> t .-target .-value)})
                    )
       ;;on change, reset the atom to be whatever is typed in
       :value (->html @text-state)
       ;;
       :style {:resize "none"
               :height "500px"
               :width "100%"}}]
     [:button
      {:on-click (fn [] 
                   (copy-to-clipboard (->html @text-state))
                   (flash "HTML copied to clipboard!")) ;;@ is how atoms are referenced
       :style {:background-color :green
               :border-radius 10
               :padding "1em"
               :color :white}}
      "Copy HTML"]]
    
    [:div
     {:style {:flex "1"
              :padding-left "2em"}}
     [:h2 "HTML Preview"]
     [:div {:dangerouslySetInnerHTML {:__html (->html @text-state)} 
            :style {:height "500px"}}]
     ]]]) 
     ;;this is the fn which provides the html that gets mounted on the real dom


(defn mount! []
  (reagent-dom/render [app]
                  (.getElementById js/document "app"))) ;;we render "app" fn's html onto the "app" element in real dom	
;;virtual dom into dom -- determines where the virtual dom mounts on the real dom

(defn main! []
  (mount!)) 
;;this is what gets called when the app is started
;;we mount when we start	

(defn reload! []
  (mount!)) 
;;and we mount when we reload, so that :after-load can reflect reagent's work
