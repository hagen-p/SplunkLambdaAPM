import os
import json
import boto3
import requests

PRICE_URL       = os.environ.get('PRICE_URL')
ORDER_LINE      = os.environ.get('ORDER_LINE')

# Define the client to interact with AWS Lambda
client = boto3.client('lambda')

def lambda_handler(event,context):
    print ("event: " , event)
    #Define / read input parameters from the event trigger
    Name         =  json.loads(event ['body']).get("ProductName")  # Value passed in from test case
    Quantity     =  json.loads(event ['body']).get("Quantity")     # Value passed in from test case
    CustomerType =  json.loads(event ['body']).get("CustomerType") # Value passed in from test case

    # Call Node-JS lambda via Api Gateway to get the Price
    if PRICE_URL:   # if there is a value we try to call the service otherwise use a dummy value
       print ("Price_url: " , PRICE_URL)
       payload = {'CustomerType': CustomerType}
       r = requests.post(PRICE_URL,  params=payload)

       #Get Price from response   
       Price =  json.loads(r.text).get('Price') # Get Value from the Price calculator  

    else:
        Price = 600. # for testing 
    
    print ("Price: " ,Price)
    if ORDER_LINE:  # if order line is set we will try to call it other wise use a dummy
        print("Order_Line_ARN: " ,ORDER_LINE)    
      # Define the input parameters that will be passed on to the line item calculation function
        inputParams = {
            "ProductName" : Name ,
            "Quantity"    : Quantity,
            "UnitPrice"   : Price
        }
        print ("inputpasrams :",inputParams)
        # Invoking Lambda directly
     
        response = client.invoke(
            FunctionName = ORDER_LINE, # This could be set as a Lambda Environment Variable
            InvocationType = 'RequestResponse',
            Payload = json.dumps(inputParams)
        )
        responseFromOrderLine = json.load(response['Payload'])
        print ("Response: ",responseFromOrderLine)
        newPrice = responseFromOrderLine.get('Amount')
        
        transactionID =  responseFromOrderLine.get('TransactionID')
    else:
        newPrice =  Price
        transactionID = "1-800-transaction-id"
        
    print("New Price: " + str(newPrice) + " transactionID: "+ transactionID)        
    responseCode = 200
    retval={'phoneType'     : Name,
            'quantity'      : Quantity,
            'customerType'  : CustomerType,
            'price'         : newPrice,
            'transaction'   : transactionID
            }
    return {
            'statusCode': responseCode,
            'body': json.dumps(retval)
            
        }