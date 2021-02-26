using System;
using Windows.UI;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Media;
using org_comroid_status_api;

// The User Control item template is documented at https://go.microsoft.com/fwlink/?LinkId=234236

namespace status_app
{
    public sealed class StatusServerBox : ServiceBox
    {
        public StatusServerBox() : base("status-server")
        {
            ServiceName = "Status Server";
        }
    }

    public partial class ServiceBox : UserControl
    {
        public static readonly DependencyProperty ServiceNameProperty = DependencyProperty.Register(
            "ServiceName",
            typeof(string),
            typeof(ServiceBox),
            new PropertyMetadata(null)
        );

        public static readonly DependencyProperty StatusTextProperty = DependencyProperty.Register(
            "StatusText",
            typeof(string),
            typeof(ServiceBox),
            new PropertyMetadata(null)
        );

        public static readonly DependencyProperty StatusColorProperty = DependencyProperty.Register(
            "StatusColor",
            typeof(Brush),
            typeof(ServiceBox),
            new PropertyMetadata(null)
        );

        public ServiceBox(string serviceName)
        {
            Name = $"status-{serviceName}";
            DataContext = this;
            InitializeComponent();
        }

        public string ServiceName
        {
            get => GetValue(ServiceNameProperty).ToString();
            internal set => SetValue(ServiceNameProperty, value);
        }

        public string StatusText
        {
            get => GetValue(StatusTextProperty).ToString();
            internal set => SetValue(StatusTextProperty, value);
        }

        internal Color StatusColor
        {
            set => SetValue(StatusColorProperty, new SolidColorBrush(value));
        }

        public void UpdateDisplay(Service service)
        {
            if (!Name.Equals($"status-{service.Name}"))
                throw new ArgumentException("Service ID mismatch");
            ServiceName = service.DisplayName;
            ServiceStatus status = service.GetStatus();
            StatusText = status.Display;
            System.Drawing.Color old = status.DisplayColor;
            StatusColor = Color.FromArgb(old.A, old.R, old.G, old.B);
        }
    }
}