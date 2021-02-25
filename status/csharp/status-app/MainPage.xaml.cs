using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;
using Windows.System;

// Die Elementvorlage "Leere Seite" wird unter https://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x407 dokumentiert.

namespace status_app
{
    /// <summary>
    /// Eine leere Seite, die eigenständig verwendet oder zu der innerhalb eines Rahmens navigiert werden kann.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        public static readonly Uri Homepage = new Uri("https://status.comroid.org");
        private StackPanel _stacker;

        public MainPage()
        {
            this.InitializeComponent();

            if (Resources.TryGetValue("StatusBox", out object value))
            {
                ControlTemplate template = value as ControlTemplate;
            }
        }

        private async void ReloadPage(object sender, RoutedEventArgs e)
        {
            throw new NotImplementedException();
        }

        private async void InitializeServiceList(object sender, RoutedEventArgs e)
        {
            _stacker = sender as StackPanel;
        }

        private async void OpenInBrowser(object sender, RoutedEventArgs e)
        {
            await Launcher.LaunchUriAsync(Homepage);
        }
    }

    public sealed class ServiceBox : Frame
    {
        private readonly Service _service;

        private ServiceBox(Service service)
        {
            _service = service;
        }
    }
}