using System;
using System.Collections.Generic;
using System.Linq;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.System;
using org_comroid_status_api;

// Die Elementvorlage "Leere Seite" wird unter https://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x407 dokumentiert.

namespace status_app
{
    /// <summary>
    /// Eine leere Seite, die eigenständig verwendet oder zu der innerhalb eines Rahmens navigiert werden kann.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        public static readonly Uri Homepage = new Uri("https://status.comroid.org");
        internal static readonly StatusConnection Connection = new StatusConnection();
        private StackPanel _stacker;

        public MainPage()
        {
            this.InitializeComponent();
        }

        private async void ReloadPage(object sender, RoutedEventArgs e)
        {
            List<Service> services = await Connection.RefreshServiceCache();

            foreach (Service service in services)
            {
                ServiceBox existing = ComputeServiceBox(service);
                existing.UpdateDisplay(service);
            }
        }

        private ServiceBox ComputeServiceBox(Service service)
        {
            return _stacker.Children
                       .Cast<ServiceBox>()
                       .FirstOrDefault(box => box.Name.Equals($"status-{service.Name}"))
                   ?? new ServiceBox(_stacker, service);
        }

        private async void InitializeServiceList(object sender, RoutedEventArgs e)
        {
            _stacker = sender as StackPanel;
            ReloadPage(sender, e);
        }

        private async void OpenInBrowser(object sender, RoutedEventArgs e)
        {
            await Launcher.LaunchUriAsync(Homepage);
        }
    }

    internal sealed class ServiceBox : Panel
    {
        private readonly TextBox _displayName;
        private readonly TextBox _statusText;

        internal ServiceBox(StackPanel stacker, Service service)
        {
            stacker.Children.Add(this);

            this._displayName = new TextBox()
            {
                Text = service.DisplayName, Style = Resources["HeaderTextBlockStyle"] as Style,
                HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center
            };
            this._statusText = new TextBox()
            {
                Text = ServiceStatus.Unknown.Display, Style = Resources["HeaderTextBlockStyle"] as Style,
                HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center
            };
            Children.Add(_displayName);
            Children.Add(_statusText);

            UpdateDisplay(service);
        }

        public void UpdateDisplay(Service service)
        {
            _statusText.Text = service.GetStatus().Display;
        }
    }
}