using System;
using System.Collections.Generic;
using System.IO;
using System.Security.Cryptography;
using System.Text;

namespace MD5DeepGui
{
    // Multi-algorithm, single-pass file hashing, mirroring how md5deep's
    // sibling tools (md5deep, sha1deep, sha256deep, ...) each cover one
    // algorithm. Each file is read once and fed to every selected algorithm.
    public class HashJobAlgorithm
    {
        public string Name;                    // display / output name, e.g. "MD5"
        public string FileToken;               // token used in hashdeep-style headers, e.g. "md5"
        public Func<HashAlgorithm> Factory;
    }

    public class FileHashResult
    {
        public string Path;
        public long Size;
        public Dictionary<string, string> Hashes = new Dictionary<string, string>();
        public string Error;
        public bool Cancelled;
    }

    public static class HashEngine
    {
        public static List<HashJobAlgorithm> AllAlgorithms()
        {
            var list = new List<HashJobAlgorithm>();
            list.Add(new HashJobAlgorithm { Name = "MD5", FileToken = "md5", Factory = delegate { return MD5.Create(); } });
            list.Add(new HashJobAlgorithm { Name = "SHA-1", FileToken = "sha1", Factory = delegate { return SHA1.Create(); } });
            list.Add(new HashJobAlgorithm { Name = "SHA-256", FileToken = "sha256", Factory = delegate { return SHA256.Create(); } });
            list.Add(new HashJobAlgorithm { Name = "SHA-512", FileToken = "sha512", Factory = delegate { return SHA512.Create(); } });
            return list;
        }

        public static FileHashResult HashFile(string path, List<HashJobAlgorithm> algorithms,
                                              Action<int> bytesProcessed, Func<bool> isCancelled)
        {
            var result = new FileHashResult();
            result.Path = path;
            var engines = new List<KeyValuePair<string, HashAlgorithm>>();
            try
            {
                foreach (var a in algorithms)
                    engines.Add(new KeyValuePair<string, HashAlgorithm>(a.Name, a.Factory()));

                using (var fs = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite, 1024 * 1024))
                {
                    result.Size = fs.Length;
                    var buffer = new byte[1024 * 1024];
                    int read;
                    while ((read = fs.Read(buffer, 0, buffer.Length)) > 0)
                    {
                        if (isCancelled != null && isCancelled())
                        {
                            result.Cancelled = true;
                            return result;
                        }
                        foreach (var e in engines)
                            e.Value.TransformBlock(buffer, 0, read, null, 0);
                        if (bytesProcessed != null)
                            bytesProcessed(read);
                    }
                    foreach (var e in engines)
                    {
                        e.Value.TransformFinalBlock(buffer, 0, 0);
                        result.Hashes[e.Key] = ToHex(e.Value.Hash);
                    }
                }
            }
            catch (Exception ex)
            {
                result.Error = ex.Message;
            }
            finally
            {
                foreach (var e in engines)
                {
                    try { e.Value.Dispose(); }
                    catch (Exception) { }
                }
            }
            return result;
        }

        public static string ToHex(byte[] data)
        {
            var sb = new StringBuilder(data.Length * 2);
            for (int i = 0; i < data.Length; i++)
                sb.Append(data[i].ToString("x2"));
            return sb.ToString();
        }

        // Recursive file collection with per-directory error capture, so one
        // access-denied folder does not abort the whole walk (md5deep -r behavior).
        public static void CollectFiles(string root, bool recursive, List<string> into, List<string> errors)
        {
            try
            {
                foreach (var f in Directory.GetFiles(root))
                    into.Add(f);
            }
            catch (Exception ex)
            {
                errors.Add(root + ": " + ex.Message);
                return;
            }
            if (!recursive)
                return;
            string[] subdirs;
            try
            {
                subdirs = Directory.GetDirectories(root);
            }
            catch (Exception ex)
            {
                errors.Add(root + ": " + ex.Message);
                return;
            }
            foreach (var d in subdirs)
                CollectFiles(d, true, into, errors);
        }

        // Accepts md5deep output ("hash  path"), hashdeep output, CSV, or bare
        // hash lists. Any whitespace/comma-separated token that looks like a
        // hex digest of a known length is treated as a known hash.
        public static HashSet<string> LoadKnownHashes(string file)
        {
            var set = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            var separators = new char[] { ' ', '\t', ',', ';' };
            foreach (var raw in File.ReadAllLines(file))
            {
                var line = raw.Trim();
                if (line.Length == 0 || line.StartsWith("#") || line.StartsWith("%"))
                    continue;
                var tokens = line.Split(separators, StringSplitOptions.RemoveEmptyEntries);
                foreach (var t in tokens)
                {
                    if (IsHexDigest(t))
                        set.Add(t.ToLowerInvariant());
                }
            }
            return set;
        }

        private static bool IsHexDigest(string s)
        {
            // MD5=32, SHA-1=40, SHA-256=64, SHA-384=96, SHA-512=128 hex chars
            if (s.Length != 32 && s.Length != 40 && s.Length != 64 && s.Length != 96 && s.Length != 128)
                return false;
            for (int i = 0; i < s.Length; i++)
            {
                char c = s[i];
                bool hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
                if (!hex)
                    return false;
            }
            return true;
        }

        public static string FormatSize(long bytes)
        {
            if (bytes < 1024) return bytes + " B";
            double kb = bytes / 1024.0;
            if (kb < 1024) return kb.ToString("0.#") + " KB";
            double mb = kb / 1024.0;
            if (mb < 1024) return mb.ToString("0.#") + " MB";
            double gb = mb / 1024.0;
            return gb.ToString("0.##") + " GB";
        }
    }
}
