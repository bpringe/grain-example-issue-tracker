(ns issue-tracker.commands
  (:require
   [ai.obney.grain.command-processor-v2.interface :refer [defcommand]]
   [ai.obney.grain.event-store-v3.interface :refer [->event]]))

;; TODO: Was not obvious what :issue really is / is for here
;; TODO: Should this be just ":issue open"?
;; TODO: Should this :issue ns be something different?
(defcommand :issue-tracker open-issue
  {:authorized? (constantly true)}
  "Opens a new issue"
  [context]
  (let [id (random-uuid)
        title (get-in context [:command :issue/title])
        body (get-in context [:command :issue/body])]
    {:command-result/events
     [(->event {:type :issue-tracker/issue-opened
                :body {:issue/id id
                       :issue/title title
                       :issue/body body}
                ;; TODO: Maybe it would be good to add :tags to the example in the core-concepts.md doc
                ;; since it's best practice to add tags
                :tags #{[:issue/id id]}})]
     :command/result {:issue/id id}}))

(comment
  (def id (random-uuid))
  (def title "Do some important thing")
  (def body "Details about the important thing")
  :rcf)
