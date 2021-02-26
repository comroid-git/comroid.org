using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Reflection;
using System.Runtime.ConstrainedExecution;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json.Serialization;
using RestSharp;

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
            req.AddHeader("Authorization", StatusConnection.Instance.Token);
            req.AddJsonBody(new StatusHolder(status.Value));
            Service yield = await Task.Run(() => StatusConnection.Instance.Rest.Execute<Service>(req).Data);
            return yield;
        }
        internal void CopyFrom(Service other)
        {
            if (Name != other.Name)
                throw new ArgumentException("ID mismatch");
            DisplayName = other.DisplayName;
            Status = other.Status;
            Url = other.Url;
        }
    }

    public sealed class ServiceStatus
    {
        public string Display { get; }
        public readonly int Value;
        public readonly Color DisplayColor;
        private static readonly List<ServiceStatus> instances = new List<ServiceStatus>();

        public static readonly Color OnlineColor = Color.FromArgb(0x91, 0xdc, 0xa3);
        public static readonly Color BusyColor = Color.FromArgb(0xf0, 0xe0, 0xa2);
        public static readonly Color MaintenanceColor = Color.FromArgb(0xfc,0x9b,0x7a);
        public static readonly Color OfflineColor = Color.FromArgb(0xde,0x68,0x68);

        public static readonly ServiceStatus Unknown = new ServiceStatus(0, "Unknown", Color.Gray);

        public static readonly ServiceStatus Offline = new ServiceStatus(1, "Offline", OfflineColor);
        public static readonly ServiceStatus Crashed = new ServiceStatus(2, "Crashed", OfflineColor);
        public static readonly ServiceStatus Maintenance = new ServiceStatus(3, "Down for Maintenance", MaintenanceColor);

        public static readonly ServiceStatus NotResponding = new ServiceStatus(4, "Not Responding", BusyColor);

        public static readonly ServiceStatus Online = new ServiceStatus(5, "Online", OnlineColor);

        private ServiceStatus(int value, string display, Color color)
        {
            Display = display;
            Value = value;
            DisplayColor = color;
            instances.Add(this);
        }

        public static ServiceStatus ValueOf(int value)
        {
            return instances.FirstOrDefault(status => status.Value == value) ?? Unknown;
        }
    }
}