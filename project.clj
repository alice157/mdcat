(defproject mdcat "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.rpl/specter "1.1.4"]
                 [com.vladsch.flexmark/flexmark-all "0.64.8"]
                 [clojure.java-time "1.2.0"]
                 [com.github.clj-easy/graal-build-time "0.1.4"]
                 [org.clojure/tools.cli "1.0.219"]
                 [mvxcvi/puget "1.3.4"]
                 [instaparse "1.4.12"]
                 [camel-snake-kebab "0.4.3"]]
  :main mdcat.main
  :target-path "target/%s"
  :profiles {:uberjar {:target-path "target/uberjar"
                       :uberjar-name "mdcat.jar"
                       :aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}})
