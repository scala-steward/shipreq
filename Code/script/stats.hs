#!/usr/bin/runghc

import Control.Applicative
import Control.Monad
import Data.List
import Data.Maybe
import Data.Monoid
import System.Directory
import System.Posix.Files
import System.Process
import Text.Printf

------------------------------------------------------------------------------------------------------------------------
-- Dirs & cloc

joinDirs :: FilePath -> FilePath -> FilePath
joinDirs "." b = b
joinDirs a   b = a ++ "/" ++ b

dirEntriesIn :: FilePath -> IO [FilePath]
dirEntriesIn d = (addPrefix . ignoreDots) <$> getDirectoryContents d
  where addPrefix = map $ joinDirs d
        ignoreDots = filter $ not . flip elem [".",".."]

dirsIn :: FilePath -> IO [FilePath]
dirsIn = (filterM isDir =<<) . dirEntriesIn

isDir :: FilePath -> IO Bool
isDir = (isDirectory <$>) . getFileStatus

cloc :: [FilePath] -> IO Stat
cloc = (parseCloc <$>) . runClocS

runClocS :: [FilePath] -> IO String
runClocS fs = p <$> readProcessWithExitCode "cloc" fs [] where p (_,stdout,_) = stdout

parseCloc :: String -> Stat
parseCloc a = parse $ listToMaybe $ filter (isPrefixOf "Scala") $ lines a
  where parse Nothing  = emptyStat
        parse (Just l) = Stat (col 1) (col 4) where col = read . (words l !!)

------------------------------------------------------------------------------------------------------------------------
-- Stats gathering

groups = ["base", "taskman", "webapp"]
mainPaths = ["src/main/scala"]
testPaths = ["src/test-lib/scala", "src/test/scala"]

type Group = String
type Module = String
type Stats = (Stat,Stat)

data GroupD = GroupD Group [(Module, Stats)] deriving (Show)

data Stat = Stat { files :: Int, loc :: Int } deriving (Show, Eq)
emptyStat = Stat 0 0
instance Monoid Stat where
  mappend a b = Stat (files a + files b) (loc a + loc b)
  mempty = emptyStat

isEmpty :: Stat -> Bool
isEmpty (Stat a b) = a==0 && b==0

areEmpty :: Stats -> Bool
areEmpty (a,b) = isEmpty a && isEmpty b

modulesFor :: Group -> [FilePath] -> [Module]
modulesFor g = sort . filter (isPrefixOf g)

statForModule :: [FilePath] -> Module -> IO Stat
statForModule dirs m = cloc $ map (joinDirs m) dirs

statsForModule :: Module -> IO Stats
statsForModule m = do a <- statForModule mainPaths m
                      b <- statForModule testPaths m
                      if isSuffixOf "-test" m
                        then return (emptyStat, mappend a b)
                        else return (a,b)

groupD :: [FilePath] -> Group -> IO GroupD
groupD dirs g = let
  a = modulesFor g dirs
  b = sequence $ map statsForModule a -- IO [Stats]
  c = zip a <$> b                     -- IO [(Module, Stats)]
  d = filter (not . areEmpty . snd) <$> c
  in GroupD g <$> d

gatherAllStats :: IO [GroupD]
gatherAllStats = do dirs <- dirsIn "."
                    sequence $ map (groupD dirs) groups

------------------------------------------------------------------------------------------------------------------------
-- Printing stats

header = "                         |       Files     |            LoC\n"
       ++"                         |    M    T    ∑  |      M      T      ∑  (T:M)\n"

float i = fromIntegral i :: Float

testRatio (Stat _ m, Stat _ t) = (float t) / (float m)

testRatioS (Stat _ 0, _) = " - "
testRatioS s = printf "%.1f" $ testRatio s

fmtG :: GroupD -> [String] -- row per module
fmtG (GroupD g ms) = flip map ms $ fmtG'
fmtG' (m,s) = printf "%-24s | %s  | %s  (%s)" m (fmtPF s) (fmtPL s) (testRatioS s)

fmtP :: String -> (Stat -> Int) -> Stats -> String
fmtP prec f (a,b) =
  let d = "%"++prec++"d"
      p = d++" "++d++" "++d
      t = mappend a b
  in printf p (f a) (f b) (f t)

fmtPF = fmtP "4" files
fmtPL = fmtP "6" loc

fmtGroups = (unlines . fmtG =<<)

groupStats (GroupD _ ms) = mconcat $ map snd ms

singleModuleGroup name stats = GroupD name [(name,stats)]

consolidateGroup gd @ (GroupD g ms) = singleModuleGroup g $ groupStats gd

projectTotalStats gs = mconcat (map groupStats gs)
projectTotal = singleModuleGroup "∑" . projectTotalStats

------------------------------------------------------------------------------------------------------------------------

main :: IO ()
main = do putStrLn "Analysing..."
          gs <- gatherAllStats -- [GroupD]
          -- putStrLn $ show gs
          putStrLn header
          putStrLn $ fmtGroups [projectTotal gs]
          putStrLn $ fmtGroups $ map consolidateGroup gs
          putStrLn $ fmtGroups gs

