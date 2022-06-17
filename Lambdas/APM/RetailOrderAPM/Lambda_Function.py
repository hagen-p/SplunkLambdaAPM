mport os
import json
import boto3
import requests

from opentelemetry import trace
def logo11y(ctx, message):
    o11yEnv = os.environ['OTEL_RESOURCE_ATTRIBUTES'].partition ('deployment.environment=')[2]
    otelSpanID = format(ctx.span_id, "016x")
    otelTraceID = format(ctx.trace_id, "032x")
    logline = {'trace_id'     :  otelTraceID,
                'span_id'     :  otelSpanID,
                'service.name' : os.environ['OTEL_SERVICE_NAME'],
                'deployment.environment' : o11yEnv
            }
    print ( json.dumps(logline) + " " + message)
    return

PRICE_URL       = os.environ['PRICE_URL']
ORDER_LINE      = os.environ['ORDER_LINE']

# Define the client to interact with AWS Lambda
client = boto3.client('lambda')


def lambda_handler(event,context):
    # grab the current span , then grab the span contect for related content
    current_span = trace.get_current_span()
    ctx = current_span.get_span_context()
    #Setup log line
    logo11y( ctx, " We recieved: " + str(event))

    #Define / read input parameters from the event trigger
    Name         =  json.loads(event ['body']).get("ProductName")  # Value passed in from test case
    Quantity     =  json.loads(event ['body']).get("Quantity")     # Value passed in from test case
    CustomerType =  json.loads(event ['body']).get("CustomerType") # Value passed in from test case
  
    # Adding tags
    current_span.set_attribute("ProductName", Name)
    current_span.set_attribute("Quantity", Quantity)
    
    # Call Node-JS lambda via Api Gateway to get the Price
    payload = {'CustomerType': CustomerType}
    r = requests.post(PRICE_URL,  params=payload)

    logo11y( ctx,"Price Url: " + r.url)
    logo11y( ctx,"Price Payload: "  + r.text)  
    #Get Price from response   
    Price =  json.loads(r.text).get('Price') # Get Value from the Price calculator  
#    Price = 600. # for testing 
    logo11y( ctx,"Price: " + str(Price))

  # Define the input parameters that will be passed on to the line item calculation function
    inputParams = {
        "ProductName" : Name ,
        "Quantity"    : Quantity,
        "UnitPrice"   : Price
    }
    print (inputParams)
    # Invoking Lambda directly
    logo11y( ctx, ORDER_LINE)

    response = client.invoke(
        FunctionName = ORDER_LINE, # This could be set as a Lambda Environment Variable
        InvocationType = 'RequestResponse',
        Payload = json.dumps(inputParams)
    )
    responseFromOrderLine = json.load(response['Payload'])
    logo11y( ctx, responseFromOrderLine)
    
    newPrice = responseFromOrderLine.get('Amount')
    logo11y( ctx,"Price: "  + str(newPrice))
    
    transactionID =  responseFromOrderLine.get('TransactionID')
    logo11y( ctx,"transactions id: " +  transactionID)
    
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
        
        