(ns cuenta.codecs
  (:require [cognitect.transit :as transit])
  #?(:clj (:import [java.io ByteArrayInputStream ByteArrayOutputStream])))

(defn transit-decode
  "decode the input request stream using transit"
  [input-stream & {:keys [msg-type] :or {msg-type :json}}]
  #?(:clj (-> (transit/reader input-stream msg-type)
              transit/read)
     :cljs (-> (transit/reader :json)
               (transit/read input-stream))))

(defn transit-encode
  "encode the response payload using transit"
  [payload & {:keys [msg-type] :or {msg-type :json}}]
  #?(:clj (let [output-stream (ByteArrayOutputStream.)
                writer (transit/writer output-stream msg-type)]
            (transit/write writer payload)
            (ByteArrayInputStream. (.toByteArray output-stream)))
     :cljs (-> (transit/writer :json)
               (transit/write payload))))
