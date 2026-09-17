#!/bin/bash

# ===== DISPLAY TABLE & READ USER INPUT CHOICE=====
printf "\n"
printf "| %-2s | %-50s |\n" "ID" "File choice"
printf "|----|----------------------------------------------------|\n"
printf "| %-2s | %-50s |\n" 0 "Search Transaction By External ID"
printf "| %-2s | %-50s |\n" 1 "Query Organization Balance"
printf "| %-2s | %-50s |\n" 2 "Initiate Transaction : Online Merchant Payment"
printf "| %-2s | %-50s |\n" 3 "Initiate Transaction : Give Change"
printf "\n"

read -p "Which soap call to make (0-3): " FILE_KEY


# ===== MAP INPUT TO VALUE =====
case "$FILE_KEY" in
  0) REQUEST_FILE="SearchTransactionByExtID" ;;
  1) REQUEST_FILE="QueryOrganizationBalance" ;;
  2) REQUEST_FILE="InitTrans_OnlineMerchantPayment" ;;
  3) REQUEST_FILE="InitTrans_GiveChange" ;;
  *) echo "Invalid choice"; exit 1 ;;
esac


# ===== SELECT APPROPRIATE URL BASED ON THE CHOICE MADE (PROD) =====
# For payment Endpoints, use >> https://172.16.52.14/payment/services/APIRequestMgrService
# For searchtransaction, use >> https://172.16.52.14/payment/services/SYNCAPIRequestMgrService
# NOTE: only the APIRequestMgrService URL for prod was provided. The SYNCAPIRequestMgrService
# host below is assumed by analogy with the test env (same host, different service path) -
# confirm with the provider before relying on it.
if [ "$FILE_KEY" -eq 0 ]; then
  URL="https://172.16.52.14/payment/services/SYNCAPIRequestMgrService"
else
  URL="https://172.16.52.14/payment/services/APIRequestMgrService"
fi


# ===== BUILD & CHECK REQUEST PATH =====
REQUEST_BASE_PATH="Endpoints_Moov_Money_PROD/"
REQUEST_FULL_PATH="$REQUEST_BASE_PATH$REQUEST_FILE.xml"

if [ ! -f "$REQUEST_FULL_PATH" ]; then
  echo "Error: Request file '$REQUEST_FULL_PATH' not found"
  exit 1
fi

if grep -q "REPLACE_WITH_" "$REQUEST_FULL_PATH"; then
  echo "Error: '$REQUEST_FULL_PATH' still contains REPLACE_WITH_* placeholders."
  echo "Edit the file with a real phone number / amount before running this against production."
  exit 1
fi


# ===== EXECUTION =====
echo ""
echo "Request (PROD):"
echo "--------------------------------------------------"
echo "Name  > $REQUEST_FILE"
echo "Url   > $URL"

OUTPUT_FILE="responses_prod/response-${REQUEST_FILE}-$(date +%Y%m%d_%H%M%S).xml"

RESPONSE=$(curl -k -s -i \
  -H "Content-Type: text/xml; charset=utf-8" \
  --data-binary @"$REQUEST_FULL_PATH" \
  "$URL" \
  -w "\nHTTP_STATUS:%{http_code}\nTOTAL_TIME:%{time_total}s\n")

# Save full raw response to file (headers + body + curl metadata)
echo "$RESPONSE" > "$OUTPUT_FILE"

# Extract status & time
HTTP_STATUS=$(echo "$RESPONSE" | grep "HTTP_STATUS:" | cut -d':' -f2)
TOTAL_TIME=$(echo "$RESPONSE" | grep "TOTAL_TIME:" | cut -d':' -f2)

# Extract XML body (remove headers + curl metadata)
BODY=$(echo "$RESPONSE" \
  | sed '/HTTP_STATUS:/,$d' \
  | sed -n '/^\r$/,$p' \
  | sed '1d')

echo ""
echo "Response:"
echo "--------------------------------------------------"

# Pretty print XML (fallback if xmllint not installed)
echo "$BODY" | xmllint --format - 2>/dev/null || echo "$BODY"

echo ""
echo "HTTP Status: $HTTP_STATUS"
echo "Total Time: $TOTAL_TIME"
echo "Saved to: $OUTPUT_FILE"
echo "--------------------------------------------"
echo "------------------ DONE --------------------"
echo "--------------------------------------------"
