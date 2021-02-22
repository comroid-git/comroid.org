using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.ConstrainedExecution;
using System.Text;
using Newtonsoft.Json;

namespace org_comroid_status_api
{
    public abstract class Entity
    {
        [JsonProperty] public string Name { get; set; }
    }

    public sealed class Service : Entity
    {
        [JsonProperty(propertyName: "display_name")]
        public string DisplayName { get; set; }

        [JsonProperty] public int Status { get; set; }

        [JsonProperty] public string Url { get; set; }

        public ServiceStatus GetStatus()
        {
            return ServiceStatus.ValueOf(Status);
        }
    }

    public sealed class ServiceStatus
    {
        private readonly int _value;
        private static readonly List<ServiceStatus> instances = new List<ServiceStatus>();

        public static readonly ServiceStatus Unknown = new ServiceStatus(0);

        public static readonly ServiceStatus Offline = new ServiceStatus(1);
        public static readonly ServiceStatus Crashed = new ServiceStatus(2);
        public static readonly ServiceStatus Maintenance = new ServiceStatus(3);

        public static readonly ServiceStatus NotResponding = new ServiceStatus(4);

        public static readonly ServiceStatus Online = new ServiceStatus(5);

        private ServiceStatus(int value)
        {
            _value = value;
            instances.Add(this);
        }

        public static ServiceStatus ValueOf(int value)
        {
            return instances.FirstOrDefault(status => status._value == value) ?? Unknown;
        }
    }
}