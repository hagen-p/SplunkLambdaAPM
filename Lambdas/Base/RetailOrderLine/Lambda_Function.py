import json
import uuid
import os

def lambda_handler(event, context):
    print ("event: " , event)
    #1 Read the input parameters
    productName = event['ProductName']
    quantity    = event['Quantity']
    unitPrice   = event['UnitPrice']

    
    #2 Generate the Order Transaction ID
    transactionId   = str(uuid.uuid1())
    print ("transactionId: "+ transactionId) 
    #3 Implement Business Logic
    amount      = quantity * unitPrice
    print ("Amount: "+ amount) 
    #4 Format and return the result
    return {
        'TransactionID' :   transactionId,
        'ProductName'   :   productName,
        'Amount'        :   amount
        
        }