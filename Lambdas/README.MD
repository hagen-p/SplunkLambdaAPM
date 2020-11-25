The lambda's can be tested independitly of the Java App by running the following curl command: (once updated with correct URL:)

curl -H "Content-Type: application/json" -X POST -d '{"ProductName": "Nokia 3212","Quantity": 12, "CustomerType": "Gold"}' https:(<replace with proper URL from API Gateway for Your retailorderservice>)RetailOrder
