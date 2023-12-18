import os, stat, argparse, shutil, json

# cli arg definitions...
parser = argparse.ArgumentParser()
parser.add_argument("-d", "--buildDir", default="../../../build/distribution", help="SFC build artifacts directory")
parser.add_argument("-n", "--compBaseName", default="com.amazon.sfc", help="Greengrass Component basename")
parser.add_argument("-v", "--compVersion", default="0.0.1", help="SFC Greengrass Component version")
parser.add_argument("-b", "--s3sfcBucket", default="s3://YOUR-S3-BUCKET", help="Greengrass Component S3 Bucket")
parser.add_argument("-p", "--compNamePrefix", default="latest", help="Component Name PrefixDir")
parser.add_argument("-s", "--compNameSuffix", default="latest", help="Component Name Suffix")
parser.add_argument("-r", "--region", default="your-aws-region", help="AWS regions where components get registered")
parser.add_argument("-a", "--accountId", default="123456789", help="Your aws accountId")
args = vars(parser.parse_args())

# vars from arg parser...
sfcBuildDir = args["buildDir"]
sfcComponentBaseName =  args["compBaseName"]
sfcComponentVersion = args["compVersion"]
sfcBucket = args["s3sfcBucket"]
compNameSuffix = args["compNameSuffix"]
compNamePrefix = args["compNamePrefix"]
region = args["region"]
accountId=args["accountId"]

for arg in args:
    print(arg+" = "+args[arg])

cmds = []
cmds2 = []

# first, create top-level components dir within `build` dir...
prefixDir = os.path.join(sfcBuildDir, compNamePrefix)
if not os.path.exists(prefixDir):
    os.mkdir(prefixDir)

# add s3 upload entry to command array
cmds.append("aws s3 cp --recursive  {topLevelComponentFolder} {s3Bucket}/{s3Path} --region {region}".format(topLevelComponentFolder=sfcBuildDir+"/"+compNamePrefix, s3Bucket=sfcBucket, s3Path=compNamePrefix, region=region))

recipeDir = os.path.join(sfcBuildDir, compNamePrefix, "recipes")
if not os.path.exists(recipeDir):
    os.mkdir(recipeDir)

def createArtifacts():
    for filename in os.listdir(sfcBuildDir):
        f = os.path.join(sfcBuildDir, filename)
        fileBaseName = filename.split(".")[0]
        # checking if it is a file
        if os.path.isfile(f):
            #now create component artifact dir
            artifactDirName = sfcComponentBaseName+"."+fileBaseName
            artifactDir = os.path.join(sfcBuildDir, compNamePrefix, "artifacts", artifactDirName, sfcComponentVersion)
            if not os.path.exists(artifactDir):
                os.makedirs(artifactDir)
            # now copy tar.gz file to that artifact dir
            if not os.path.exists(os.path.join(artifactDir, filename)):
                shutil.copy(f, artifactDir)
            #print(artifactDir)


def moduleRecipe():
    with open("resources/sfc-module-recipe.json.template", "r") as file:
        json_data = json.load(file)
    return json_data

def sfcMainRecipe():
    with open("resources/sfc-main-recipe.json.template", "r") as file:
        json_data = json.load(file)
    return json_data

def writeJsonRecipe(recipe, recipeFilePath):
    with open(recipeFilePath, "w", encoding='utf-8') as outfile:
        json.dump(recipe, outfile, indent=4)


def createRecipes():
    for filename in os.listdir(sfcBuildDir):
        f = os.path.join(sfcBuildDir, filename)
        fileBaseName = filename.split(".")[0]
        # checking if it is a file
        if os.path.isfile(f):
            #now create component recipes
            recipeFileName = sfcComponentBaseName+"."+fileBaseName+"-"+sfcComponentVersion+".json"
            recipeFile = os.path.join(sfcBuildDir, compNamePrefix, "recipes", recipeFileName)
            if fileBaseName == "sfc-main":
                recipe = sfcMainRecipe()
            else:
                recipe = moduleRecipe()
            recipe["ComponentName"] = sfcComponentBaseName+"."+fileBaseName
            recipe["ComponentVersion"] = sfcComponentVersion
            recipe["ComponentDescription"] = "SFC "+fileBaseName+" module"
            recipe["Manifests"][0]["Lifecycle"]["Install"]["Script"] = "cd {artifacts:path} && tar -xvf "+filename
            if fileBaseName == "sfc-main":
                recipe["Manifests"][0]["Lifecycle"]["Run"]["Script"] = "printf '{configuration:/SFC_CONFIG_JSON}' > {artifacts:path}/conf.json && {artifacts:path}/"+fileBaseName+"/bin/"+fileBaseName+" -config {artifacts:path}/conf.json"
            else:
                recipe["Manifests"][0]["Lifecycle"]["Run"]["Script"] = "if $IPC_MODE; then {artifacts:path}/"+fileBaseName+"/bin/"+fileBaseName+" -port {configuration:/ipc_port}; fi"
            recipe["Manifests"][0]["Artifacts"][0]["URI"] = sfcBucket+"/"+compNamePrefix+"/artifacts/"+sfcComponentBaseName+"."+fileBaseName+"/"+sfcComponentVersion+"/"+filename
            writeJsonRecipe(recipe, recipeFile)
            cmds.append(("aws greengrassv2 create-component-version  --inline-recipe fileb://{recipeFile} --region {region}").format(recipeFile=recipeFile, region=region))
            cmds2.append("aws greengrassv2 delete-component --arn arn:aws:greengrass:{region}:{accountId}:components:{compName}:versions:{compVersion} --region {region}"
                         .format(region=region, accountId=accountId, compName=sfcComponentBaseName+"."+fileBaseName, compVersion=sfcComponentVersion))

def createFileFromCmds(filename, cmds):
    with open(filename, "w", encoding='utf-8') as outfile:
        for cmd in cmds:
            #print(cmd)
            outfile.write(cmd+"\n")
    os.chmod(filename, stat.S_IRWXU)

def cliOutputs():
    installer="install-sfc-components"
    uninstaller="delete-sfc-components"
    createFileFromCmds(installer+".sh", cmds)
    createFileFromCmds(installer+".bat", cmds)
    createFileFromCmds(uninstaller+".sh", cmds2)
    createFileFromCmds(uninstaller+".bat", cmds2)
   
    print("")
    print("--> In order to install into your aws account run: ./{name}".format(name=installer+".sh|bat"))
    print("--> In order to uninstall all sfc components run : ./{name}".format(name=uninstaller+".sh|bat"))
    print("")
    


if __name__ == '__main__':
    createArtifacts()
    createRecipes()
    cliOutputs()
   