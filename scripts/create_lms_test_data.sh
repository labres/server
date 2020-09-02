#!/bin/bash

BASIC_AUTH_USER=hmx
BASIC_AUTH_PASSWORD=test
HOST=:8080
ISSUER_ID=hmx
EVENT=sp-1

ORDER_NUMBER_1=$RANDOM
ORDER_NUMBER_2=$RANDOM
ORDER_NUMBER_3=$RANDOM
ORDER_NUMBER_4=$RANDOM
ORDER_NUMBER_5=$RANDOM
ORDER_NUMBER_6=$RANDOM
ORDER_NUMBER_7=$RANDOM
ORDER_NUMBER_8=$RANDOM
ORDER_NUMBER_9=$RANDOM

TEST_SITE_1="hmx-1"
TEST_SITE_2="hmx-2"
TEST_SITE_3="hmx-3"


function register_order {
    http -v post $HOST/v1/issuers/$ISSUER_ID/orders \
    orderNumber=$1 \
    testSiteId=$2 \
    sampledAt=$(expr $EPOCHSECONDS \* 1000) \
    metadata:="{\"event\":\"$3\", \"ticket\":\"$4\"}"
}

function upload_result {
    http -v -a $BASIC_AUTH_USER:$BASIC_AUTH_PASSWORD put $HOST/v1/results \
    issuerId==$ISSUER_ID \
    orderNumber=$1 \
    result=$2
}

register_order $ORDER_NUMBER_1 $TEST_SITE_1 $EVENT $RANDOM
register_order $ORDER_NUMBER_2 $TEST_SITE_2 $EVENT $RANDOM
register_order $ORDER_NUMBER_3 $TEST_SITE_1 $EVENT $RANDOM
register_order $ORDER_NUMBER_4 $TEST_SITE_2 $EVENT $RANDOM
register_order $ORDER_NUMBER_5 $TEST_SITE_1 "another" $RANDOM
register_order $ORDER_NUMBER_6 $TEST_SITE_2 "another" $RANDOM
register_order $ORDER_NUMBER_7 $TEST_SITE_1 $EVENT $RANDOM
register_order $ORDER_NUMBER_8 $TEST_SITE_2 $EVENT $RANDOM
register_order $ORDER_NUMBER_9 $TEST_SITE_3 $EVENT $RANDOM

upload_result $ORDER_NUMBER_1 "NEGATIVE"
upload_result $ORDER_NUMBER_2 "NEGATIVE"
upload_result $ORDER_NUMBER_3 "POSITIVE"
upload_result $ORDER_NUMBER_5 "NEGATIVE"
upload_result $ORDER_NUMBER_7 "NEGATIVE"
upload_result $ORDER_NUMBER_9 "NEGATIVE"