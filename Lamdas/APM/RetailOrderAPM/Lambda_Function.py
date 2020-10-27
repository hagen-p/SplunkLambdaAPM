import signalfx_lambda
import opentracing

import os
import json
import boto3
import urllib3
import urllib.request

# The Environment Tag is used by Splunk APM to filter Environments in UI
APM_ENVIRONMENT = os.environ['SIGNALFX_APM_ENVIRONMENT']
PRICE_URL       = os.environ['PRICE_URL']
ORDER_LINE       = os.environ['ORDER_LINE']

# Define the client to interact with AWS Lambda
client = boto3.client('lambda')

@signalfx_lambda.emits_metrics()
@signalfx_lambda.is_traced()
def lambda_handler(event,context):
     # Setup tracer so we can create spans and retrieve the B3 Headers
    tracer = opentracing.tracer
    TraceHeaders = {} # Here we will store the B3 Headers needed for manual Propagation if required
    signalfx_lambda.tracing.inject(TraceHeaders) # Retrieving B3 Headers and injecting them into the trace header array
    span = tracer.active_span #grabiing the Active span for Custom Tags
    print(event)
   
     # Define / read input parameters from the event trigger
    Name         =  json.loads(event ['body']).get("ProductName")  # Value passed in from test case
    Quantity     =  json.loads(event ['body']).get("Quantity")     # Value passed in from test case
    CustomerType =  json.loads(event ['body']).get("CustomerType") # Value passed in from test case
  
    # Adding tags
    span.set_tag("environment" , APM_ENVIRONMENT) # Usefull in APM to make sure it matches your expected environement
    span.set_tag("ProductName", Name)
    span.set_tag("Quantity", Quantity)
    
       # Call Node-JS lambda via Api Gateway to get the Price
    http = urllib3.PoolManager()
    r = http.request('GET', PRICE_URL +"?CustomerType="+CustomerType , headers=TraceHeaders)
    
    #Get Price from response   
    Price = json.loads(r.data.decode('utf-8')).get("Price") # Get Value from the Price calculator
   
     #set tag with price   
    span.set_tag("UnitPrice", Price)
    
    # Define the input parameters that will be passed on to the child function
    inputParams = {
        "ProductName" : Name ,
        "Quantity"    : Quantity,
        "UnitPrice"   : Price,
        "TraceHeaders"  : TraceHeaders # Add the TraceHeaders as an input parameter so it can be used by the Lambda being called
    }
    print (inputParams)
    # Invoking Lambda directly
    response = client.invoke(
        FunctionName = ORDER_LINE, # This is set as a Lambda Environment Variable
        InvocationType = 'RequestResponse',
        Payload = json.dumps(inputParams)
    )

    responseFromOrderLine = json.load(response['Payload'])

    #long line can be send to the span by using log
    span.log_kv({'order-line response': responseFromOrderLine})
    #optionally close the span
    span.finish()
    return {
            'phoneType'     : Name,
            'quantity'      : Quantity,
            'customerType'  : CustomerType,
            'price'         : responseFromOrderLine.get('Amount'),
            'transaction' : responseFromOrderLine.get('TransactionID')
        }