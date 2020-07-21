/*
 * Copyright 2013-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.cxx;

import com.facebook.buck.android.AndroidPackageable;
import com.facebook.buck.android.AndroidPackageableCollector;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.Flavor;
import com.facebook.buck.model.Pair;
import com.facebook.buck.python.PythonPlatform;
import com.facebook.buck.python.PythonPackageComponents;
import com.facebook.buck.rules.AddToRuleKey;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.BuildTargetSourcePath;
import com.facebook.buck.rules.HasRuntimeDeps;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.TargetGraph;
import com.facebook.buck.rules.coercer.FrameworkPath;
import com.facebook.buck.rules.args.Arg;
import com.facebook.buck.rules.args.SourcePathArg;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * An action graph representation of a C/C++ library from the target graph, providing the
 * various interfaces to make it consumable by C/C++ preprocessing and native linkable rules.
 */
public class CxxLibrary extends AbstractCxxLibrary implements HasRuntimeDeps {

  @AddToRuleKey
  private final boolean canBeAsset;

  private final BuildRuleParams params;
  private final BuildRuleResolver ruleResolver;
  private final Iterable<? extends NativeLinkable> exportedDeps;
  private final Predicate<CxxPlatform> headerOnly;
  private final Function<? super CxxPlatform, ImmutableMultimap<CxxSource.Type, String>>
      exportedPreprocessorFlags;
  private final Function<? super CxxPlatform, ImmutableList<Arg>> exportedLinkerFlags;
  private final Optional<Pattern> supportedPlatformsRegex;
  private final ImmutableSet<FrameworkPath> frameworks;
  private final ImmutableSet<FrameworkPath> libraries;
  private final Linkage linkage;
  private final boolean linkWhole;
  private final Optional<String> soname;
  private final ImmutableSortedSet<BuildTarget> tests;

  private final Map<Pair<Flavor, HeaderVisibility>, ImmutableMap<BuildTarget, CxxPreprocessorInput>>
      cxxPreprocessorInputCache = Maps.newHashMap();

  public CxxLibrary(
      BuildRuleParams params,
      BuildRuleResolver ruleResolver,
      SourcePathResolver pathResolver,
      Iterable<? extends NativeLinkable> exportedDeps,
      Predicate<CxxPlatform> headerOnly,
      Function<? super CxxPlatform, ImmutableMultimap<CxxSource.Type, String>>
          exportedPreprocessorFlags,
      Function<? super CxxPlatform, ImmutableList<Arg>> exportedLinkerFlags,
      Optional<Pattern> supportedPlatformsRegex,
      ImmutableSet<FrameworkPath> frameworks,
      ImmutableSet<FrameworkPath> libraries,
      Linkage linkage,
      boolean linkWhole,
      Optional<String> soname,
      ImmutableSortedSet<BuildTarget> tests,
      boolean canBeAsset) {
    super(params, pathResolver);
    this.params = params;
    this.ruleResolver = ruleResolver;
    this.exportedDeps = exportedDeps;
    this.headerOnly = headerOnly;
    this.exportedPreprocessorFlags = exportedPreprocessorFlags;
    this.exportedLinkerFlags = exportedLinkerFlags;
    this.supportedPlatformsRegex = supportedPlatformsRegex;
    this.frameworks = frameworks;
    this.libraries = libraries;
    this.linkage = linkage;
    this.linkWhole = linkWhole;
    this.soname = soname;
    this.tests = tests;
    this.canBeAsset = canBeAsset;
  }

  private boolean isPlatformSupported(CxxPlatform cxxPlatform) {
    return !supportedPlatformsRegex.isPresent() ||
        supportedPlatformsRegex.get()
            .matcher(cxxPlatform.getFlavor().toString())
            .find();
  }

  @Override
  public CxxPreprocessorInput getCxxPreprocessorInput(
      TargetGraph targetGraph,
      CxxPlatform cxxPlatform,
      HeaderVisibility headerVisibility) {
    return CxxPreprocessables.getCxxPreprocessorInput(
        targetGraph,
        params,
        ruleResolver,
        cxxPlatform.getFlavor(),
        headerVisibility,
        CxxPreprocessables.IncludeType.LOCAL,
        exportedPreprocessorFlags.apply(cxxPlatform),
        frameworks);
  }

  @Override
  public ImmutableMap<BuildTarget, CxxPreprocessorInput> getTransitiveCxxPreprocessorInput(
      TargetGraph targetGraph,
      CxxPlatform cxxPlatform,
      HeaderVisibility headerVisibility) {
    Pair<Flavor, HeaderVisibility> key = new Pair<>(cxxPlatform.getFlavor(), headerVisibility);
    ImmutableMap<BuildTarget, CxxPreprocessorInput> result = cxxPreprocessorInputCache.get(key);
    if (result == null) {
      Map<BuildTarget, CxxPreprocessorInput> builder = Maps.newLinkedHashMap();
      builder.put(
          getBuildTarget(),
          getCxxPreprocessorInput(targetGraph, cxxPlatform, headerVisibility));
      for (BuildRule dep : getDeps()) {
        if (dep instanceof CxxPreprocessorDep) {
          builder.putAll(
              ((CxxPreprocessorDep) dep).getTransitiveCxxPreprocessorInput(
                  targetGraph,
                  cxxPlatform,
                  headerVisibility));
        }
      }
      result = ImmutableMap.copyOf(builder);
      cxxPreprocessorInputCache.put(key, result);
    }
    return result;
  }

  @Override
  public Iterable<NativeLinkable> getNativeLinkableDeps(CxxPlatform cxxPlatform) {
    return FluentIterable.from(getDeclaredDeps())
        .filter(NativeLinkable.class);
  }

  @Override
  public Iterable<? extends NativeLinkable> getNativeLinkableExportedDeps(CxxPlatform cxxPlatform) {
    return exportedDeps;
  }

  @Override
  public NativeLinkableInput getNativeLinkableInput(
      TargetGraph targetGraph,
      CxxPlatform cxxPlatform,
      Linker.LinkableDepType type) {

    if (!isPlatformSupported(cxxPlatform)) {
      return NativeLinkableInput.of();
    }

    if (headerOnly.apply(cxxPlatform)) {
      return NativeLinkableInput.of(
          ImmutableList.<Arg>of(),
          Preconditions.checkNotNull(frameworks),
          ImmutableSet.<FrameworkPath>of());
    }

    // Build up the arguments used to link this library.  If we're linking the
    // whole archive, wrap the library argument in the necessary "ld" flags.
    ImmutableList.Builder<Arg> linkerArgsBuilder = ImmutableList.builder();
    linkerArgsBuilder.addAll(Preconditions.checkNotNull(exportedLinkerFlags.apply(cxxPlatform)));

    if (type != Linker.LinkableDepType.SHARED || linkage == Linkage.STATIC) {
      BuildRule rule =
          requireBuildRule(
              targetGraph,
              cxxPlatform.getFlavor(),
              type == Linker.LinkableDepType.STATIC ?
                  CxxDescriptionEnhancer.STATIC_FLAVOR :
                  CxxDescriptionEnhancer.STATIC_PIC_FLAVOR);
      Arg library =
          new SourcePathArg(getResolver(), new BuildTargetSourcePath(rule.getBuildTarget()));
      if (linkWhole) {
        Linker linker = cxxPlatform.getLd();
        linkerArgsBuilder.addAll(linker.linkWhole(library));
      } else {
        linkerArgsBuilder.add(library);
      }
    } else {
      BuildRule rule =
          requireBuildRule(
              targetGraph,
              cxxPlatform.getFlavor(),
              CxxDescriptionEnhancer.SHARED_FLAVOR);
      linkerArgsBuilder.add(
          new SourcePathArg(getResolver(), new BuildTargetSourcePath(rule.getBuildTarget())));
    }
    final ImmutableList<Arg> linkerArgs = linkerArgsBuilder.build();

    return NativeLinkableInput.of(
        linkerArgs,
        Preconditions.checkNotNull(frameworks),
        Preconditions.checkNotNull(libraries));
  }

  public BuildRule requireBuildRule(
      TargetGraph targetGraph,
      Flavor ... flavors) {
    return CxxDescriptionEnhancer.requireBuildRule(targetGraph, params, ruleResolver, flavors);
  }

  @Override
  public NativeLinkable.Linkage getPreferredLinkage(CxxPlatform cxxPlatform) {
    return linkage;
  }

  @Override
  public PythonPackageComponents getPythonPackageComponents(
      TargetGraph targetGraph,
      PythonPlatform pythonPlatform,
      CxxPlatform cxxPlatform) {
    if (headerOnly.apply(cxxPlatform)) {
      return PythonPackageComponents.of();
    }
    if (linkage == Linkage.STATIC) {
      return PythonPackageComponents.of();
    }
    if (!isPlatformSupported(cxxPlatform)) {
      return PythonPackageComponents.of();
    }
    ImmutableMap.Builder<Path, SourcePath> libs = ImmutableMap.builder();
    String sharedLibrarySoname = CxxDescriptionEnhancer.getSharedLibrarySoname(
        soname,
        getBuildTarget(),
        cxxPlatform);
    BuildRule sharedLibraryBuildRule = requireBuildRule(
        targetGraph,
        cxxPlatform.getFlavor(),
        CxxDescriptionEnhancer.SHARED_FLAVOR);
    libs.put(
        Paths.get(sharedLibrarySoname),
        new BuildTargetSourcePath(sharedLibraryBuildRule.getBuildTarget()));
    return PythonPackageComponents.of(
        /* modules */ ImmutableMap.<Path, SourcePath>of(),
        /* resources */ ImmutableMap.<Path, SourcePath>of(),
        /* nativeLibraries */ libs.build(),
        /* prebuiltLibraries */ ImmutableSet.<SourcePath>of(),
        /* zipSafe */ Optional.<Boolean>absent());
  }

  @Override
  public Iterable<AndroidPackageable> getRequiredPackageables() {
    return AndroidPackageableCollector.getPackageableRules(params.getDeps());
  }

  @Override
  public void addToCollector(AndroidPackageableCollector collector) {
    if (canBeAsset) {
      collector.addNativeLinkableAsset(this);
    } else {
      collector.addNativeLinkable(this);
    }
  }

  @Override
  public ImmutableMap<String, SourcePath> getSharedLibraries(
      TargetGraph targetGraph,
      CxxPlatform cxxPlatform) {
    if (headerOnly.apply(cxxPlatform)) {
      return ImmutableMap.of();
    }
    if (linkage == Linkage.STATIC) {
      return ImmutableMap.of();
    }
    if (!isPlatformSupported(cxxPlatform)) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<String, SourcePath> libs = ImmutableMap.builder();
    String sharedLibrarySoname = CxxDescriptionEnhancer.getSharedLibrarySoname(
        soname,
        getBuildTarget(),
        cxxPlatform);
    BuildRule sharedLibraryBuildRule = requireBuildRule(
        targetGraph,
        cxxPlatform.getFlavor(),
        CxxDescriptionEnhancer.SHARED_FLAVOR);
    libs.put(
        sharedLibrarySoname,
        new BuildTargetSourcePath(sharedLibraryBuildRule.getBuildTarget()));
    return libs.build();
  }

  @Override
  public boolean isTestedBy(BuildTarget testTarget) {
    return tests.contains(testTarget);
  }

  @Override
  public ImmutableSortedSet<BuildRule> getRuntimeDeps() {
    // We export all declared deps as runtime deps, to setup a transitive runtime dep chain which
    // will pull in runtime deps (e.g. other binaries) or transitive C/C++ libraries.  Since the
    // `CxxLibrary` rules themselves are noop meta rules, they shouldn't add any unnecessary
    // overhead.
    return getDeclaredDeps();
  }

}
