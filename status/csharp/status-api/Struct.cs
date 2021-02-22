using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.ConstrainedExecution;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using RestSharp;

namespace org_comroid_status_api
{
    public abstract class Entity
    {
        protected readonly StatusConnection Connection;

        protected Entity(StatusConnection connection)
        {
            this.Connection = connection;
        }

        [JsonProperty] public string Name { get; set; }
    }

    public sealed class Service : Entity
    {
        public Service(StatusConnection connection) : base(connection)
        {
        }

        [JsonProperty(propertyName: "display_name")]
        public string DisplayName { get; set; }

        [JsonProperty] public int Status { get; set; }

        [JsonProperty] public string Url { get; set; }

        public ServiceStatus GetStatus()
        {
            return ServiceStatus.ValueOf(Status);
        }

        public sealed class StatusHolder
        {
            [JsonProperty] public int status { get; private set; }

            internal StatusHolder(int status)
            {
                this.status = status;
            }
        }

        public async Task<Service> UpdateStatus(ServiceStatus status)
        {
            RestRequest req = new RestRequest($"service/{Name}/status", Method.POST, DataFormat.Json);
            req.AddHeader("Authorization", Connection.Token);
            req.AddJsonBody(new StatusHolder(status.Value));
            Service yield = await Task.Run(() => Connection._rest.Execute<Service>(req).Data);
            return yield;
        }
    }

    public sealed class ServiceStatus
    {
        public readonly int Value;
        private static readonly List<ServiceStatus> instances = new List<ServiceStatus>();

        public static readonly ServiceStatus Unknown = new ServiceStatus(0);

        public static readonly ServiceStatus Offline = new ServiceStatus(1);
        public static readonly ServiceStatus Crashed = new ServiceStatus(2);
        public static readonly ServiceStatus Maintenance = new ServiceStatus(3);

        public static readonly ServiceStatus NotResponding = new ServiceStatus(4);

        public static readonly ServiceStatus Online = new ServiceStatus(5);

        private ServiceStatus(int value)
        {
            Value = value;
            instances.Add(this);
        }

        public static ServiceStatus ValueOf(int value)
        {
            return instances.FirstOrDefault(status => status.Value == value) ?? Unknown;
        }
    }
}