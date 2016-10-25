(ns cookiewars.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [cookiewars.core-test]))

(doo-tests 'cookiewars.core-test)

