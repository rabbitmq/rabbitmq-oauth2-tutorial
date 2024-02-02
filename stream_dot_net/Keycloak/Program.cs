// You need RabbitMQ.Stream.Client 1.8.0-rc.2 or later
// RabbitMQ 3.13.0 or later
// Keycloak RabbitMQ stream client example
// NOTE: By default there is not TTL for the access token, you need to set it up in the Keycloak realm
// for the client producer
using System.Buffers;
using System.Net;
using System.Text;
using RabbitMQ.Stream.Client;
using RabbitMQ.Stream.Client.Reliable;

Console.WriteLine("Keycloak Example: RabbitMQ.Stream.Client");

var system = await StreamSystem.Create(new StreamSystemConfig()
{
    UserName = "producer",
    Password = await NewAccessToken(),
    VirtualHost = "/",
});


const string stream = "test-keycloak";
await system.CreateStream(new StreamSpec(stream)
{
    MaxLengthBytes = 1_000_000,
});

var start = DateTime.Now;
var completed = new TaskCompletionSource<bool>();

_ = Task.Run(async () =>
{
    while (completed.Task.Status != TaskStatus.RanToCompletion)
    {
        await Task.Delay(TimeSpan.FromSeconds(1));
        // Suppose you have the access token for 60 seconds
        if (start.AddSeconds(50) >= DateTime.Now) continue;
        // Here we are updating the secret to the pool
        Console.WriteLine($"{DateTime.Now} - Updating the secret....");
        await system.UpdateSecret(await NewAccessToken()).ConfigureAwait(false);
        start = DateTime.Now;
    }
});

var consumer = await Consumer.Create(new ConsumerConfig(system, stream)
{
    OffsetSpec = new OffsetTypeFirst(),
    MessageHandler = (_, _, _, message) =>
    {
        Console.WriteLine(
            $"{DateTime.Now} - Received: {Encoding.UTF8.GetString(message.Data.Contents.ToArray())} ");
        return Task.CompletedTask;
    }
});


var producer = await Producer.Create(new ProducerConfig(system, stream));
// Here we are sending 10 messages per second for 5 minutes
// Given the access token is valid for 60 seconds, we should see the token being updated
// and the producer continuing to send messages
for (var i = 0; i < 10 * 5; i++)
{
    await producer.Send(new Message(Encoding.UTF8.GetBytes($"Hello KeyCloak! {i}")));
    await Task.Delay(TimeSpan.FromSeconds(1));
    Console.WriteLine($"{DateTime.Now} - Sent: Hello KeyCloak! {i}");
}

completed.SetResult(true);
Console.WriteLine("Closing...");
await consumer.Close();
await producer.Close();
await system.Close();
Console.WriteLine("Closed.");
return;

async Task<string> NewAccessToken()
{
    var data = new[]
    {
        new KeyValuePair<string?, string?>("client_id", "producer"),
        new KeyValuePair<string?, string?>("client_secret", "kbOFBXI9tANgKUq8vXHLhT6YhbivgXxn"),
        new KeyValuePair<string?, string?>("grant_type", "client_credentials"),
    };
    var httpRequestMessage = new HttpRequestMessage
    {
        Method = HttpMethod.Post,
        RequestUri = new Uri("http://localhost:8080/realms/test/protocol/openid-connect/token"),
        Headers =
        {
            {
                HttpRequestHeader.ContentType.ToString(), "application/x-www-form-urlencoded"
            },
        },
        Content = new FormUrlEncodedContent(data)
    };
    var client = new HttpClient();
    var response = await client.SendAsync(httpRequestMessage);
    var responseString = await response.Content.ReadAsStringAsync();
    var json = System.Text.Json.JsonDocument.Parse(responseString);
    var r = json.RootElement.GetProperty("access_token").GetString();
    return r ?? throw new Exception("no access token");
}


