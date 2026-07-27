using System;
using System.Reflection;
using System.Windows.Forms;

[assembly: AssemblyTitle("Hash Sidekick")]
[assembly: AssemblyDescription("A Windows sidekick for the md5deep workflow. md5deep was created by Jesse Kornblum; Hash Sidekick is an independent companion app and is not affiliated with the original project.")]
[assembly: AssemblyProduct("Hash Sidekick")]
[assembly: AssemblyCopyright("Public domain, like md5deep itself. Original md5deep by Jesse Kornblum.")]
[assembly: AssemblyVersion("1.0.0.0")]
[assembly: AssemblyFileVersion("1.0.0.0")]

namespace HashSidekick
{
    internal static class Program
    {
        [STAThread]
        private static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new MainForm());
        }
    }
}
