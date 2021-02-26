using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.Graphics.Printing.OptionDetails;
using Windows.UI;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;
using org_comroid_status_api;

// The User Control item template is documented at https://go.microsoft.com/fwlink/?LinkId=234236

namespace status_app
{
    public sealed partial class StatusServerBox : ServiceBox
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

        public ServiceBox(string serviceName)
        {
            Name = $"status-{serviceName}";
            DataContext = this;
            this.InitializeComponent();
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