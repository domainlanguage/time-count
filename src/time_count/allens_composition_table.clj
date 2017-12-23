(ns time-count.allens-composition-table
  (:require [clojure.set :refer [map-invert]])
  )

(def full #{:equal
            :before :after
            :meets :met-by
            :starts :started-by
            :finishes :finished-by
            :during :contains
            :overlaps :overlapped-by})

(comment "too verbose"
         (def composition-of-basic-relations [r s]
           (case [r s]
             [:before :before] #{:before}
             [:meets :before] #{:before}
             [:overlaps :before] #{:before}
             [:finished-by :before] #{:before}
             [:contains :before] #{:before :meets :overlaps :finished-by :contains}
             )
           ))

(def mathy-to-readable
  {:e :equal
   :p :before
   :P :after
   :m :meets
   :M :met-by
   :s :starts
   :S :started-by
   :f :finishes
   :F :finished-by
   :d :during
   :D :contains
   :o :overlaps
   :O :overlapped-by})

(def readable-to-mathy
  (map-invert mathy-to-readable))

(defn composition-of-basic-relations-concise
  "composition table found on https://www.ics.uci.edu/~alspaugh/cls/shr/allen.html
  (among other places). It has 13x13 entries, and for now
  is incomplete."
  [r1 r2]
  (case [r1 r2]
    [:p :p] #{:p}
    [:p :m] #{:p}
    [:p :o] #{:p}
    [:p :F] #{:p}
    [:p :D] #{:p}
    [:p :s] #{:p}
    [:p :e] #{:p}
    [:p :S] #{:p}
    [:p :d] #{:p :m :o :s :d}
    [:p :f] #{:p :m :o :s :d}
    [:p :O] #{:p :m :o :s :d}
    [:p :M] #{:p :m :o :s :d}
    [:p :P] full

    [:m :p] #{:p}
    [:m :m] #{:p}
    [:m :o] #{:p}
    [:m :F] #{:p}
    [:m :D] #{:p}
    [:m :s] #{:m}
    [:m :e] #{:m}
    [:m :S] #{:m}
    [:m :d] #{:o :s :d}
    [:m :f] #{:o :s :d}
    [:m :O] #{:o :s :d}
    [:m :M] #{:F :e :f}
    [:m :P] #{:D :S :O :M :P}

    ;; Tired of typing ...
    ;; Skipping some that I don't use right now

    [:F :p] #{:p}
    [:F :m] #{:m}
    [:F :o] #{:o}
    [:F :F] #{:F}
    [:F :D] #{:D}
    [:F :s] #{:o}
    [:F :e] #{:F}
    [:F :S] #{:D}
    [:F :d] #{:o :s :d}
    [:F :f] #{:F :e :f}
    [:F :O] #{:D :S :O}
    [:F :M] #{:D :S :O}
    [:F :P] #{:D :S :O :M :P}

    ;; ... Skipping some more

    [:S :p] #{:p :m :o :F :D}
    [:S :m] #{:o :F :D}
    [:S :o] #{:o :F :D}
    [:S :F] #{:D}
    [:S :D] #{:D}
    [:S :s] #{:s :e :S}
    [:S :e] #{:S}
    [:S :S] #{:S}
    [:S :d] #{:d :f :O}
    [:S :f] #{:O}
    [:S :O] #{:O}
    [:S :M] #{:M}
    [:S :P] #{:P}

    [:M :p] #{:p :m :o :F :D}
    [:M :m] #{:s :e :S}
    [:M :o] #{:d :f :O}
    [:M :F] #{:M}
    [:M :D] #{:P}
    [:M :s] #{:d :f :O}
    [:M :e] #{:M}
    [:M :S] #{:P}
    [:M :d] #{:d :f :O}
    [:M :f] #{:M}
    [:M :O] #{:P}
    [:M :M] #{:P}
    [:M :P] #{:P}

    )
  )

